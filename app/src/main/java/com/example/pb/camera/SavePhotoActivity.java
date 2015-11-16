package com.example.pb.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class SavePhotoActivity extends Activity {

    private ImageView photoView;
    private Button savePhotoButton;
    private AlertDialog saveDialog;

    private SharedPreferences settings;
    private static final String APP_PREFERENCES = "preferences";
    private static final String APP_PREFERENCES_FILENAME = "filename";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WriteFileIntentService.ERROR_ACTION:
                    int errorMsg;
                    switch (intent.getIntExtra(WriteFileIntentService.ERROR_TYPE, -1)) {
                        case WriteFileIntentService.ERROR:
                            errorMsg = R.string.closing_error;
                            break;
                        case WriteFileIntentService.WRITING_FILE_ERROR:
                            errorMsg = R.string.writing_file_error;
                            break;
                        default:
                            errorMsg = R.string.unknown_error;
                            break;
                    }
                    Toast.makeText(SavePhotoActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
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

        File photo = new File(getFilesDir(), getResources().getString(R.string.temp_filename));

        if (photo.exists()) {
            photoView.setImageBitmap(decodeSampledBitmap(100, 100));
        } else {
            Toast.makeText(this, R.string.no_photo_error, Toast.LENGTH_SHORT).show();
            finish();
        }

        savePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog().show();
            }
        });

        settings = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        if (settings.contains(APP_PREFERENCES_FILENAME)) {
            createDialog().show();
            ((EditText)saveDialog.findViewById(R.id.filename)).setText(settings.getString(APP_PREFERENCES_FILENAME, ""));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        SharedPreferences.Editor editor = settings.edit();
        if (isChangingConfigurations()) {
            if (saveDialog != null) {
                editor.putString(APP_PREFERENCES_FILENAME, ((EditText) saveDialog.findViewById(R.id.filename)).getText().toString());
            } else {
                editor.remove(APP_PREFERENCES_FILENAME);
            }
        } else {
            editor.remove(APP_PREFERENCES_FILENAME);
        }
        editor.apply();
    }

    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.save_dialog, null))
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = ((EditText) saveDialog.findViewById(R.id.filename)).getText().toString();

                        startService(new Intent(SavePhotoActivity.this, WriteFileIntentService.class)
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

    private Bitmap decodeSampledBitmap(int reqWidth, int reqHeight) {
        File file = new File(getFilesDir(), getResources().getString(R.string.temp_filename));

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);


        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
