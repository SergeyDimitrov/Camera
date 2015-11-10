package com.example.pb.camera;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.FileOutputStream;

public class WriteFileIntentService extends IntentService {

    public static final String FILENAME_KEY = "filename_key";
    public static final String IMAGE_KEY = "image_key";
    public static final String ERROR_ACTION = "error_action";

    public WriteFileIntentService() {
        super("WriteFileIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Bitmap photo = (Bitmap)intent.getExtras().getParcelable(IMAGE_KEY);
            String filename = intent.getExtras().getString(FILENAME_KEY);
            String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures" + "/" + filename + ".jpg";

            if (filename.equals("")) {
                sendErrorMessage();
                return;
            }

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                photo.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                sendErrorMessage();
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                } catch (Exception e) {
                    sendErrorMessage();
                }

            }
        }
    }

    private void sendErrorMessage() {
        sendBroadcast(new Intent().setAction(ERROR_ACTION));
    }

}
