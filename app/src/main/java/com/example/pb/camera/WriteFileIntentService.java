package com.example.pb.camera;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.io.FileOutputStream;

public class WriteFileIntentService extends IntentService {

    public static final String PATH_KEY = "path_key";
    public static final String IMAGE_KEY = "image_key";

    public WriteFileIntentService() {
        super("WriteFileIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Bitmap photo = (Bitmap)intent.getExtras().getParcelable(IMAGE_KEY);
            String path = intent.getExtras().getString(PATH_KEY);

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                photo.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }

            }
        }
    }
}
