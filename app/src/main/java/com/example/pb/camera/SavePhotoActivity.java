package com.example.pb.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class SavePhotoActivity extends Activity {

    private ImageView photoView;
    private Button savePhotoButton;
    private AlertDialog saveDialog;

    private byte[] compressedPhoto;

    private RetainedFragment dataFragment;

    private static final String RETAIN_FILENAME_TAG = "retain_filename";
    public static final String PHOTO_KEY = "photo";


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WriteFileIntentService.ERROR_ACTION:
                    Toast.makeText(SavePhotoActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    break;
                case WriteFileIntentService.SUCCESS_ACTION:
                    Toast.makeText(SavePhotoActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_photo);
        photoView = (ImageView)findViewById(R.id.photo_view);
        savePhotoButton = (Button)findViewById(R.id.save_photo_button);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WriteFileIntentService.ERROR_ACTION);
        filter.addAction(WriteFileIntentService.SUCCESS_ACTION);
        registerReceiver(receiver, filter);

        compressedPhoto = getIntent().getExtras().getByteArray(PHOTO_KEY);
        if (compressedPhoto != null) {
            photoView.setImageBitmap(BitmapFactory.decodeByteArray(compressedPhoto, 0, compressedPhoto.length));
        } else {
            Toast.makeText(this, R.string.no_photo_error, Toast.LENGTH_SHORT).show();
        }

        savePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog().show();
            }
        });

        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag(RETAIN_FILENAME_TAG);
        if (dataFragment == null) {
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment, RETAIN_FILENAME_TAG).commit();
        }

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
            if (saveDialog != null) {
                dataFragment.setFilename(((EditText)saveDialog.findViewById(R.id.filename)).getText().toString());
            } else {
                dataFragment.setFilename(null);
            }
        }
    }

    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.save_dialog, null))
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = ((EditText) saveDialog.findViewById(R.id.filename)).getText().toString();

                        startService(new Intent(SavePhotoActivity.this, WriteFileIntentService.class)
                                        .putExtra(WriteFileIntentService.IMAGE_KEY, compressedPhoto)
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
