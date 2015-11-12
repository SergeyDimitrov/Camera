package com.example.pb.camera;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

public class CameraActivity extends Activity {

    private Button shotButton;
    private Button saveButton;
    private Button changeCameraButton;

    private Camera camera;
    private SurfaceHolder holder;
    private Bitmap photo;


    private int cameraID = 0;
    private static final String CAMERA_ID_KEY = "camera_id_key";

    private RetainedFragment dataFragment;

    private static final String RETAIN_PHOTO_TAG = "retain_photo_tag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        shotButton = (Button)findViewById(R.id.shot_button);
        saveButton = (Button)findViewById(R.id.save_button);
        changeCameraButton = (Button)findViewById(R.id.change_camera_button);

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
                Log.d("myTAG", "surfaceCreated");
                try {
                    if (camera != null) camera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    Toast.makeText(CameraActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                Log.d("myTAG", "surfaceCreatedEnd");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d("myTAG", "surfaceChanged");
                try {
                    camera.startPreview();
                } catch (Exception e) {
                    Toast.makeText(CameraActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                Log.d("myTAG", "surfaceChangedEnd");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d("myTAG", "surfaceDestroyed");
                if (camera != null) camera.stopPreview();
                else Log.d("myTag", "No need");
                Log.d("myTAG", "surfaceDestroyedEnd");
            }
        });

        cameraPreviewLayout.addView(preview);

        // Setting listeners

        shotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableButtons(false);
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Log.d("myTAG", "Start");
                        photo = BitmapFactory.decodeByteArray(data, 0, data.length);

                        Log.d("myTAG", "End");
                        camera.startPreview();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableButtons(true);
                            }
                        });
                    }
                });
                Log.d("myTAG", "Picture taken");
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableButtons(false);
                if (photo == null) {
                    Toast.makeText(CameraActivity.this, R.string.no_photo_error, Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Compress photo
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            byte[] bytes = stream.toByteArray();

                            startActivity(new Intent(CameraActivity.this, SavePhotoActivity.class)
                                    .putExtra(SavePhotoActivity.PHOTO_KEY, bytes));
                        }
                    }).start();

                }
                enableButtons(true);
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
                    Toast.makeText(CameraActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                enableButtons(true);
            }
        });

        // Retaining data

        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag(RETAIN_PHOTO_TAG);

        if (dataFragment == null) {
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment, RETAIN_PHOTO_TAG).commit();
        }

        photo = dataFragment.getImage();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(CAMERA_ID_KEY, cameraID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("myTAG", "onResume");
        camera = getCameraInstance();
        Log.d("myTAG", "onResumeEnd");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("myTAG", "onPause");
        releaseCamera();
        camera = null;
        Log.d("myTAG", "onPauseEnd");
    }


    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isChangingConfigurations()) {
            dataFragment.setImage(photo);
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
        saveButton.setEnabled(enable);
        shotButton.setEnabled(enable);
        changeCameraButton.setEnabled(enable);
    }

}
