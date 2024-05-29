package com.japg.mastermoviles.opengl10;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OpenGLActivity extends AppCompatActivity {
	private GLSurfaceView glSurfaceView;
	private boolean rendererSet = false;
	private ScaleGestureDetector scaleGestureDetector;
	private GestureDetector gestureDetector;
	private float scaleFactor = 1.0f;

	private float previousX;
	private float previousY;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Crear un RelativeLayout para contener el GLSurfaceView y el SeekBar
		RelativeLayout layout = new RelativeLayout(this);

		glSurfaceView = new GLSurfaceView(this);
		final OpenGLRenderer openGLRenderer = new OpenGLRenderer(this);
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
				|| (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
				&& (Build.FINGERPRINT.startsWith("generic")
				|| Build.FINGERPRINT.startsWith("unknown")
				|| Build.MODEL.contains("google_sdk")
				|| Build.MODEL.contains("Emulator")
				|| Build.MODEL.contains("Android SDK built for x86")));

		if (supportsEs2) {
			glSurfaceView.setEGLContextClientVersion(2);
			glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
			glSurfaceView.setRenderer(openGLRenderer);
			rendererSet = true;
			Toast.makeText(this, "OpenGL ES 2.0 soportado", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Este dispositivo no soporta OpenGL ES 2.0", Toast.LENGTH_LONG).show();
			return;
		}

		scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				scaleFactor /= detector.getScaleFactor();
				scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
				glSurfaceView.queueEvent(() -> openGLRenderer.setScaleFactor(scaleFactor));
				return true;
			}
		});

		gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				final float normalizedX = (e2.getX() / (float) glSurfaceView.getWidth()) * 2 - 1;
				final float normalizedY = -((e2.getY() / (float) glSurfaceView.getHeight()) * 2 - 1);
				glSurfaceView.queueEvent(() -> openGLRenderer.handleTouchDrag(normalizedX, normalizedY));
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				final float normalizedX = (e.getX() / (float) glSurfaceView.getWidth()) * 2 - 1;
				final float normalizedY = -((e.getY() / (float) glSurfaceView.getHeight()) * 2 - 1);
				glSurfaceView.queueEvent(() -> openGLRenderer.handleTouchPress(normalizedX, normalizedY));
				return true;
			}
		});

		glSurfaceView.setOnTouchListener((v, event) -> {
			if (event != null) {
				scaleGestureDetector.onTouchEvent(event);
				gestureDetector.onTouchEvent(event);

				if (event.getAction() == MotionEvent.ACTION_POINTER_UP) {
					int actionIndex = event.getActionIndex();
					int pointerId = event.getPointerId(actionIndex);
					if (pointerId == 0) {
						previousX = (event.getX(1) / (float) v.getWidth()) * 2 - 1;
						previousY = -((event.getY(1) / (float) v.getHeight()) * 2 - 1);
					} else {
						previousX = (event.getX(0) / (float) v.getWidth()) * 2 - 1;
						previousY = -((event.getY(0) / (float) v.getHeight()) * 2 - 1);
					}
				}
				return true;
			} else {
				return false;
			}
		});


		SeekBar seekBar = new SeekBar(this);
		seekBar.setMax(100);
		seekBar.setProgress(50);
		RelativeLayout.LayoutParams seekBarLayoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT
		);
		seekBarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		seekBar.setLayoutParams(seekBarLayoutParams);

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				float rotationY = (progress - 50) * 3.6f;
				glSurfaceView.queueEvent(() -> openGLRenderer.setHeadRotationY(rotationY));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		layout.addView(glSurfaceView);
		layout.addView(seekBar);

		setContentView(layout);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (rendererSet) {
			glSurfaceView.onPause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (rendererSet) {
			glSurfaceView.onResume();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
