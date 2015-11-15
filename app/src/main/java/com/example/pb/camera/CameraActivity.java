package com.example.pb.camera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends Activity {

    private ImageButton shotButton;
    private ImageButton changeCameraButton;

    private Camera camera;
    private SurfaceHolder holder;

    private int cameraID = 0;
    private static final String CAMERA_ID_KEY = "camera_id_key";

    private static final int delta = 10;
    private OrientationEventListener orientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        shotButton = (ImageButton)findViewById(R.id.shot_button);
        changeCameraButton = (ImageButton)findViewById(R.id.change_camera_button);

        if (savedInstanceState != null) {
            cameraID = savedInstanceState.getInt(CAMERA_ID_KEY);
        }

        // Creating camera preview

        FrameLayout cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);
        SurfaceView preview = new SurfaceView(this);
        holder = preview.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (camera != null) camera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    Toast.makeText(CameraActivity.this, R.string.preview_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                try {
                    camera.startPreview();
                } catch (Exception e) {
                    Toast.makeText(CameraActivity.this, R.string.preview_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) camera.stopPreview();
            }
        });

        cameraPreviewLayout.addView(preview);

        // Setting listeners

        orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            public void onOrientationChanged(int angle) {
                if (angle > 270 - delta && angle < 270 + delta) {
                    rotateButtons(270);
                } else if (angle > 90 - delta && angle < 90 + delta) {
                    rotateButtons(90);
                } else if (angle > 360 - delta || angle < delta) {
                    rotateButtons(0);
                }
            }
        };


        shotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableButtons(false);
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        // Writing to internal storage
                        FileOutputStream out = null;
                        try {
                            out = openFileOutput(getResources().getString(R.string.temp_filename), MODE_PRIVATE);
                            out.write(data);
                        } catch (IOException e) {
                            Toast.makeText(CameraActivity.this, R.string.writing_file_error, Toast.LENGTH_SHORT).show();
                        } finally {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    enableButtons(true);
                                }
                            });
                            try {
                                if (out != null) {
                                    out.flush();
                                    out.close();
                                }
                            } catch (IOException e) {
                                Toast.makeText(CameraActivity.this, R.string.closing_error, Toast.LENGTH_SHORT).show();
                            }
                        }

                        camera.startPreview();

                        startActivity(new Intent(CameraActivity.this, SavePhotoActivity.class));

                    }
                });
                Log.d("myTAG", "Picture taken");
            }
        });


        changeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableButtons(false);
                cameraID = (cameraID + 1) % Camera.getNumberOfCameras();
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                }
                camera = getCameraInstance();

                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                    Log.d("myTAG", "Switching camera successed");
                } catch (Exception e) {
                    Log.d("myTAG", "Switching camera failed");
                    Toast.makeText(CameraActivity.this, R.string.preview_error, Toast.LENGTH_SHORT).show();
                }
                enableButtons(true);
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(CAMERA_ID_KEY, cameraID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientationListener.enable();
        camera = getCameraInstance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationListener.disable();
        releaseCamera();
        camera = null;
    }


    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(cameraID);
        } catch (Exception e) {
            Toast.makeText(this, R.string.camera_not_available_error, Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    private void enableButtons(boolean enable) {
        shotButton.setEnabled(enable);
        changeCameraButton.setEnabled(enable);
    }

    private void rotateButtons(int angle) {
        int shotID = -1;
        int changeID = -1;
        switch (angle) {
            case 0:
                changeID = R.drawable.change_down;
                shotID = R.drawable.shot_down;
                break;
            case 90:
                changeID = R.drawable.change_left;
                shotID = R.drawable.shot_left;
                break;
            case 270:
                changeID = R.drawable.change_right;
                shotID = R.drawable.shot_right;
                break;
            default:
                Toast.makeText(this, R.string.orientation_error, Toast.LENGTH_SHORT).show();
                return;
        }
        changeCameraButton.setImageDrawable(getResources().getDrawable(changeID));
        shotButton.setImageDrawable(getResources().getDrawable(shotID));
    }

}
