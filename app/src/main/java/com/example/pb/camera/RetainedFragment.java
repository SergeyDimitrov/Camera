package com.example.pb.camera;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;

public class RetainedFragment extends Fragment {
    private Bitmap image;
    private String filename;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}
