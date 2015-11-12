package com.example.pb.camera;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;

import java.io.FileOutputStream;

public class WriteFileIntentService extends IntentService {

    public static final String FILENAME_KEY = "filename_key";
    public static final String IMAGE_KEY = "image_key";
    public static final String SUCCESS_ACTION = "success_action";
    public static final String ERROR_ACTION = "error_action";

    public WriteFileIntentService() {
        super("WriteFileIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            boolean success = true;
            byte[] photo = intent.getExtras().getByteArray(IMAGE_KEY);

            if (photo == null) {
                sendErrorMessage();
                return;
            }

            String filename = intent.getExtras().getString(FILENAME_KEY);
            String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/" + filename + ".jpg";

            if (filename == null || filename.equals("")) {
                sendErrorMessage();
                return;
            }

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                out.write(photo);
            } catch (Exception e) {
                sendErrorMessage();
                success = false;
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                } catch (Exception e) {
                    sendErrorMessage();
                    success = false;
                }
            }
            if (success) {
                sendSuccessMessage();
            }
        }
    }

    private void sendSuccessMessage() {
        sendBroadcast(new Intent().setAction(SUCCESS_ACTION));
    }

    private void sendErrorMessage() {
        sendBroadcast(new Intent().setAction(ERROR_ACTION));
    }

}
