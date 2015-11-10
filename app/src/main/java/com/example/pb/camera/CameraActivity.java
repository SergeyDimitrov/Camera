package com.example.pb.camera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;

public class CameraActivity extends AppCompatActivity {
    private Button shotButton;
    private ImageView photoImageView;
    private Bitmap photo;
    private RetainedFragment dataFragment;
    private AlertDialog saveDialog;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(CameraActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
        }
    };

    private static final String RETAIN_TAG = "retain_tag";

    private static final int REQUEST_TAKE_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        shotButton = (Button)findViewById(R.id.shot_button);
        photoImageView = (ImageView)findViewById(R.id.camera_image);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WriteFileIntentService.ERROR_ACTION);
        registerReceiver(receiver, filter);

        shotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        });

        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag(RETAIN_TAG);

        if (dataFragment == null) {
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment, RETAIN_TAG).commit();
        }
        photo = dataFragment.getImage();
        if (photo != null) photoImageView.setImageBitmap(photo);
        if (dataFragment.getFilename() != null) {
            createDialog().show();
            ((EditText)saveDialog.findViewById(R.id.filename)).setText(dataFragment.getFilename());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (isChangingConfigurations()) {
            dataFragment.setImage(photo);
            if (saveDialog != null) {
                dataFragment.setFilename(((EditText)saveDialog.findViewById(R.id.filename)).getText().toString());
            } else {
                dataFragment.setFilename(null);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            photo = (Bitmap)data.getExtras().get("data");
            photoImageView.setImageBitmap(photo);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save_image_item) {
            if (photo == null) {
                Toast.makeText(this, R.string.no_photo_error, Toast.LENGTH_SHORT).show();
            } else {
                createDialog().show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.save_dialog, null))
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = ((EditText)saveDialog.findViewById(R.id.filename)).getText().toString();

                        startService(new Intent(CameraActivity.this, WriteFileIntentService.class)
                                        .putExtra(WriteFileIntentService.IMAGE_KEY, photo)
                                        .putExtra(WriteFileIntentService.FILENAME_KEY, filename)
                        );
                        saveDialog.cancel();
                        saveDialog = null;
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveDialog.cancel();
                        saveDialog = null;
                    }
                });
        return saveDialog = builder.create();
    }
}
