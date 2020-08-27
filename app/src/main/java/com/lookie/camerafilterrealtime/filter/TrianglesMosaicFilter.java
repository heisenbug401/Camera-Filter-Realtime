package com.lookie.camerafilterrealtime.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lookie.camerafilterrealtime.MyGLUtils;
import com.lookie.camerafilterrealtime.R;

public class TrianglesMosaicFilter extends CameraFilter {
  private int program;

  public TrianglesMosaicFilter(Context context) {
    super(context);

    // Build shaders
    program = MyGLUtils.buildProgram(context, R.raw.vertext, R.raw.triangles_mosaic);
  }

  @Override
  public void onDraw(int cameraTexId, int canvasWidth, int canvasHeight) {
    setupShaderInputs(program,
        new int[]{canvasWidth, canvasHeight},
        new int[]{cameraTexId},
        new int[][]{});
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
  }
}
