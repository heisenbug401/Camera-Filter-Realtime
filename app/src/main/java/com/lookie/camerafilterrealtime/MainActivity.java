package com.lookie.camerafilterrealtime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.lookie.camerafilterrealtime.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
    EasyPermissions.PermissionCallbacks {

  private static final int REQUEST_CAMERA_PERMISSION = 101;

  private static final int REQUEST_APPLICATION_DETAILS_SETTINGS = 102;

  private static final String SPF_PATH = "last_photo_path";

  private static final String TAG = MainActivity.class.getSimpleName();

  private FrameLayout container;

  private CameraRenderer renderer;

  private TextureView textureView;

  private int filterId = R.id.filter0;

  private int mCurrentFilterId = 0;

  private final String[] TITLES = {
      "Original", "EdgeDectection", "Pixelize",
      "EMInterference", "TrianglesMosaic", "Legofied",
      "TileMosaic", "Blueorange", "ChromaticAberration",
      "BasicDeform", "Contrast", "NoiseWarp", "Refraction",
      "Mapping", "Crosshatch", "LichtensteinEsque",
      "AsciiArt", "MoneyFilter", "Cracked", "Polygonization",
      "JFAVoronoi", "BlackAndWhite", "Gray", "Negative",
      "Nostalgia", "Casting", "Relief", "Swirl", "HexagonMosaic",
      "Mirror", "Triple", "Cartoon", "WaterReflection"
  };

  private final Integer[] FILTER_RES_IDS = {
      R.id.filter0, R.id.filter1, R.id.filter2, R.id.filter3, R.id.filter4,
      R.id.filter5, R.id.filter6, R.id.filter7, R.id.filter8, R.id.filter9, R.id.filter10,
      R.id.filter11, R.id.filter12, R.id.filter13, R.id.filter14, R.id.filter15, R.id.filter16,
      R.id.filter17, R.id.filter18, R.id.filter19, R.id.filter20,
      R.id.filter21, R.id.filter22, R.id.filter23, R.id.filter24,
      R.id.filter25, R.id.filter26, R.id.filter27, R.id.filter28,
      R.id.filter29, R.id.filter30, R.id.filter31, R.id.filter32};

  private ArrayList<Integer> mFilterArray = new ArrayList<>(Arrays.asList(FILTER_RES_IDS));

  private GestureDetector mGestureDetector;

  private ActivityMainBinding binding;

  private FiltersAdapter adapter;

  private MediaPlayer mediaPlayer;

  private SharedPreferences sharedpreferences;

  private boolean soundOn = false;

  private String mLastImagePath;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    View decorView = getWindow().getDecorView();
    // Hide the status bar.
    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
    decorView.setSystemUiVisibility(uiOptions);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    container = binding.container;

    if (Build.VERSION.SDK_INT >= 23) {
      String[] perms = {
          Manifest.permission.CAMERA,
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE
      };
      if (EasyPermissions.hasPermissions(this, perms)) {
        setupCameraPreviewView();
      } else {
        EasyPermissions.requestPermissions(this,
            getString(R.string.permissions_required_msg), REQUEST_CAMERA_PERMISSION, perms);
      }
    } else {
      setupCameraPreviewView();
    }

    mediaPlayer = MediaPlayer.create(this, R.raw.camera_shutter);

    sharedpreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

    mGestureDetector = new GestureDetector(this, this);

    LinearLayoutManager lm = new LinearLayoutManager(this);
    lm.setOrientation(LinearLayoutManager.HORIZONTAL);
    binding.rvFilters.setLayoutManager(lm);
    binding.rvFilters.setHasFixedSize(true);

    adapter = new FiltersAdapter(this::handleSelectedFilter);

    ArrayList<FilterItem> dataSet = new ArrayList<>();
    for (int i = 0; i < mFilterArray.size(); i++) {
      dataSet.add(new FilterItem(TITLES[i], mFilterArray.get(i)));
    }

    adapter.dataSet = dataSet;
    binding.rvFilters.setAdapter(adapter);
    adapter.notifyDataSetChanged();

    loadLastPhoto();
  }

  private void loadLastPhoto() {
    String path = sharedpreferences.getString(SPF_PATH, null);
    Log.w(TAG, "path: " + path);
    if (path != null) {
      mLastImagePath = path;
      File imageFile = new File(path);
      if (imageFile.exists()) {
        Glide.with(this)
            .load(imageFile)
            .centerCrop()
            .placeholder(R.drawable.ic_placeholder)
            .into(binding.thumb);
      }
    }
  }

  private void handleSelectedFilter(FilterItem filter, int index) {
    boolean isNext = adapter.currentFilterIndex < index;

    filterId = filter.filterResId;
    if (renderer != null) {
      renderer.setSelectedFilter(filterId);
    }
    mCurrentFilterId = mFilterArray.indexOf(filterId);

    adapter.currentFilterIndex = mCurrentFilterId;
    adapter.notifyDataSetChanged();

    if (isNext && mCurrentFilterId + 1 <= adapter.getItemCount()) {
      binding.rvFilters.scrollToPosition(mCurrentFilterId + 1);
    } else if (mCurrentFilterId - 1 >= 0) {
      binding.rvFilters.scrollToPosition(mCurrentFilterId - 1);
    }
  }

  private void showDialogSettings() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.permissions_required);
    builder.setMessage(R.string.permissions_required_msg);
    builder.setCancelable(false);

    builder.setPositiveButton(R.string.settings, (dialog, id) -> {
      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.parse("package:" + getPackageName()));
      startActivityForResult(intent, REQUEST_APPLICATION_DETAILS_SETTINGS);
    });

    builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
      dialog.dismiss();
      finish();
    });

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
    }
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    // Forward results to EasyPermissions
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }

  @Override
  public void onPermissionsGranted(int requestCode, @NonNull List<String> list) {
    // Some permissions have been granted
    setupCameraPreviewView();
  }

  @Override
  public void onPermissionsDenied(int requestCode, @NonNull List<String> list) {
    // Some permissions have been denied
  }

  @SuppressLint({"ClickableViewAccessibility", "CheckResult"})
  void setupCameraPreviewView() {
    renderer = new CameraRenderer(this);
    textureView = new TextureView(this);
    container.addView(textureView);
    textureView.setSurfaceTextureListener(renderer);

    textureView.setOnTouchListener((view, motionEvent) -> {
      mGestureDetector.onTouchEvent(motionEvent);
      return true;
    });

    textureView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight()));

    binding.thumb.setOnClickListener(view -> {
      if (!TextUtils.isEmpty(mLastImagePath)) {
        Log.w(TAG, "lastPath: " + mLastImagePath);
        Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", new File(mLastImagePath));
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
      }
    });

    binding.capture.setOnClickListener(view -> {

      if (soundOn) {
        mediaPlayer.start();
      }

      binding.capture.setEnabled(false);

      Flowable<String> runBackground = Flowable.fromCallable(this::capture).subscribeOn(Schedulers.io());
      Flowable<String> showForeground = runBackground.observeOn(AndroidSchedulers.mainThread());
      Disposable disposable = showForeground.subscribe(s -> {
            Log.w(TAG, "subscribe path: " + s);
            mLastImagePath = s;
            loadLastPhoto();
            if (renderer != null) {
              renderer.setSelectedFilter(filterId);
            }
            mCurrentFilterId = mFilterArray.indexOf(filterId);
            binding.capture.setEnabled(true);
          },
          throwable -> {
            Log.e(TAG, "throwable " + throwable.getMessage());
            binding.capture.setEnabled(true);
          });
    });
  }

  private String capture() {

    String path = genSaveFileName(getTitle().toString() + "_", ".png");
    File imageFile = new File(path);
    if (imageFile.exists()) {
      imageFile.delete();
    }

    // create bitmap screen capture
    Bitmap bitmap = textureView.getBitmap();
    OutputStream outputStream;

    try {
      outputStream = new FileOutputStream(imageFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }

    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)));

    sharedpreferences.edit().putString(SPF_PATH, path).apply();

    return path;
  }

  private String genSaveFileName(String prefix, String suffix) {
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    String timeString = sdf.format(date);

    File storageDir = new File(Environment.getExternalStorageDirectory(), "Camera Filter Realtime");
    if (!storageDir.exists()) {
      storageDir.mkdirs();
    }

    return storageDir.getAbsolutePath() + "/" + prefix + timeString + suffix;
  }

  @Override
  public boolean onDown(MotionEvent motionEvent) {
    return false;
  }

  @Override
  public void onShowPress(MotionEvent motionEvent) {

  }

  @Override
  public boolean onSingleTapUp(MotionEvent motionEvent) {
    return false;
  }

  @Override
  public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
    return false;
  }

  @Override
  public void onLongPress(MotionEvent motionEvent) {

  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    float velocity = Math.abs(velocityX) > Math.abs(velocityY) ? velocityX : velocityY;
    int step = velocity > 0 ? -1 : 1;
    mCurrentFilterId = circleLoop(TITLES.length, mCurrentFilterId, step);
    if (renderer != null) {
      renderer.setSelectedFilter(FILTER_RES_IDS[mCurrentFilterId]);
    }

    adapter.currentFilterIndex = mCurrentFilterId;
    adapter.notifyDataSetChanged();

    if (step == 1 && mCurrentFilterId + 1 <= adapter.getItemCount()) {
      binding.rvFilters.scrollToPosition(mCurrentFilterId + 1);
    } else if (mCurrentFilterId - 1 >= 0) {
      binding.rvFilters.scrollToPosition(mCurrentFilterId - 1);
    }

    return true;
  }

  private int circleLoop(int size, int currentPos, int step) {
    if (step == 0) {
      return currentPos;
    }

    if (step > 0) {
      if (currentPos + step >= size) {
        return (currentPos + step) % size;
      } else {
        return currentPos + step;
      }
    } else {
      if (currentPos + step < 0) {
        return currentPos + step + size;
      } else {
        return currentPos + step;
      }
    }
  }
}
