package com.example.pb.camera;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class WriteFileIntentService extends IntentService {

    public static final String FILENAME_KEY = "filename_key";

    private static final int BUFFER_SIZE = 1024;

    public static final String SUCCESS_ACTION = "success_action";
    public static final String ERROR_ACTION = "error_action";
    public static final String ERROR_TYPE = "error_type";

    public static final int ERROR = 1;
    public static final int WRITING_FILE_ERROR = 2;

    public WriteFileIntentService() {
        super("WriteFileIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            boolean success = true;
            String filename = intent.getExtras().getString(FILENAME_KEY);
            String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/" + filename + ".jpg";

            if (filename == null || filename.equals("")) {
                sendErrorMessage(WRITING_FILE_ERROR);
                return;
            }

            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(new File(getFilesDir(), getResources().getString(R.string.temp_filename)));
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                out = new FileOutputStream(path);

                while((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }

            } catch (Exception e) {
                sendErrorMessage(WRITING_FILE_ERROR);
                success = false;
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    sendErrorMessage(ERROR);
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

    private void sendErrorMessage(int error) {
        sendBroadcast(new Intent().setAction(ERROR_ACTION).putExtra(ERROR_TYPE, error));
    }

}