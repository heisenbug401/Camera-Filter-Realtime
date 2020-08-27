package com.lookie.camerafilterrealtime;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.lookie.camerafilterrealtime.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

  private static final int REQUEST_CAMERA_PERMISSION = 101;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    container = binding.container;

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
        Toast.makeText(this, "Camera access is required.", Toast.LENGTH_SHORT).show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
            REQUEST_CAMERA_PERMISSION);
      }
    } else {
      setupCameraPreviewView();
    }

    mGestureDetector = new GestureDetector(this, this);

    LinearLayoutManager lm = new LinearLayoutManager(this);
    lm.setOrientation(LinearLayoutManager.HORIZONTAL);
    binding.rvFilters.setLayoutManager(lm);
    binding.rvFilters.setHasFixedSize(true);

    adapter = new FiltersAdapter((filter, index) -> {

      boolean isNext = adapter.currentFilterIndex < index;

      filterId = filter.filterResId;
      if (renderer != null) {
        renderer.setSelectedFilter(filterId);
      }
      mCurrentFilterId = mFilterArray.indexOf(filterId);

      adapter.currentFilterIndex = mCurrentFilterId;
      adapter.notifyDataSetChanged();

      if (isNext) {
        if (mCurrentFilterId + 1 <= adapter.getItemCount()) {
          binding.rvFilters.scrollToPosition(mCurrentFilterId + 1);
        }
      } else {
        if (mCurrentFilterId - 1 >= 0) {
          binding.rvFilters.scrollToPosition(mCurrentFilterId - 1);
        }
      }
    });

    ArrayList<FilterItem> dataSet = new ArrayList<>();
    for (int i = 0; i < mFilterArray.size(); i++) {
      dataSet.add(new FilterItem(TITLES[i], mFilterArray.get(i)));
    }

    adapter.dataSet = dataSet;
    binding.rvFilters.setAdapter(adapter);
    adapter.notifyDataSetChanged();

    mediaPlayer = MediaPlayer.create(this, R.raw.camera_shutter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
    // be trying to set app to immersive mode before it's ready and the flags do not stick
    container.postDelayed(() -> {
      View decorView = getWindow().getDecorView();
      // Hide the status bar.
      int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
      decorView.setSystemUiVisibility(uiOptions);
    }, 500L);
  }

  @Override
  protected void onDestroy() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
    }
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CAMERA_PERMISSION: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          setupCameraPreviewView();
        }
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
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

    binding.capture.setOnClickListener(view -> {
      // mediaPlayer.start();
      binding.capture.setEnabled(false);
      String path = capture();
      Toast.makeText(this,
          !path.equals("") ? "The capture has been saved to " + path :
              "Save failed!",
          Toast.LENGTH_SHORT).show();
      if (renderer != null) {
        renderer.setSelectedFilter(filterId);
      }
      mCurrentFilterId = mFilterArray.indexOf(filterId);
      binding.capture.setEnabled(true);
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
