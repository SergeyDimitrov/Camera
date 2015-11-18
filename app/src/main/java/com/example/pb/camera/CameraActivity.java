package com.example.pb.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Debug;
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
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class CameraActivity extends Activity {

    private ImageButton shotButton;
    private ImageButton changeCameraButton;

    private Camera camera;
    private SurfaceHolder holder;

    private int cameraID = 0;
    private static final String CAMERA_ID_KEY = "camera_id_key";

    private static final int delta = 10;
    private OrientationEventListener orientationListener;
    private int deviceAngle;

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
        final SurfaceView preview = new SurfaceView(this);
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
                    Camera.Parameters params = camera.getParameters();
                    Camera.Size bestSize = getBestSize(params.getSupportedPreviewSizes(), width, height);
                    params.setPreviewSize(bestSize.width, bestSize.height);
                    camera.setParameters(params);
                    camera.startPreview();
                } catch (Exception e) {
                    Toast.makeText(CameraActivity.this, R.string.preview_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) camera.stopPreview();
            }

            private Camera.Size getBestSize(List<Camera.Size> sizes, int width, int height) {
                final double ASPECT_TOLERANCE = 0.2;
                double targetRatio = (double) width / height;
                if (sizes == null) {
                    return null;
                }
                Camera.Size optimalSize = null;
                double minDiff = Double.MAX_VALUE;
                int targetHeight = height;

                for(Camera.Size size : sizes) {
                    double ratio = (double) size.width / size.height;
                    if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
                    if (Math.abs(size.height - targetHeight) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }

                if (optimalSize == null) {
                    minDiff = Double.MAX_VALUE;
                    for(Camera.Size size : sizes) {
                        if (Math.abs(size.height - targetHeight) < minDiff) {
                            optimalSize = size;
                            minDiff = Math.abs(size.height - targetHeight);
                        }
                    }
                }
                return optimalSize;
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

                        Matrix rotationMatrix = new Matrix();
                        int rotationAngle = 0;

                        switch (deviceAngle) {
                            case 0:
                                rotationAngle = (cameraID == 0 ? 90 : 270);
                                break;
                            case 270:
                                rotationAngle = 0;
                                break;
                            case 90:
                                rotationAngle = 180;
                                break;
                        }

                        rotationMatrix.postRotate(rotationAngle);

                        Bitmap sourceImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Bitmap rotatedImage = Bitmap.createBitmap(sourceImage, 0, 0, sourceImage.getWidth(),
                                sourceImage.getHeight(), rotationMatrix, true);

                        FileOutputStream out = null;
                        try {
                            out = openFileOutput(getResources().getString(R.string.temp_filename), MODE_PRIVATE);
                            rotatedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            Log.d("myTAG", "Saved to internal storage");
                        } catch (IOException e) {
                            Toast.makeText(CameraActivity.this, R.string.writing_file_error, Toast.LENGTH_SHORT).show();
                        } finally {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    enableButtons(true);
                                }
                            });

                            if (rotatedImage != null) {
                                rotatedImage.recycle();
                                rotatedImage = null;
                                Log.d("myTAG", "Rotated image recycled");
                            }

                            if (sourceImage != null) {
                                sourceImage.recycle();
                                sourceImage = null;
                                Log.d("myTAG", "Source image recycled");
                            }

                            try {
                                if (out != null) {
                                    out.flush();
                                    out.close();
                                    Log.d("myTAG", "Closed streams to internal storage");
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
        int shotID;
        int changeID;
        deviceAngle = angle;
        switch (angle) {
            case 0:
                changeID = R.drawable.change_right;
                shotID = R.drawable.shot_right;
                break;
            case 90:
                changeID = R.drawable.change_up;
                shotID = R.drawable.shot_up;
                break;
            case 270:
                changeID = R.drawable.change_down;
                shotID = R.drawable.shot_down;
                break;
            default:
                Toast.makeText(this, R.string.orientation_error, Toast.LENGTH_SHORT).show();
                return;
        }
        changeCameraButton.setImageDrawable(getResources().getDrawable(changeID));
        shotButton.setImageDrawable(getResources().getDrawable(shotID));
    }

}
