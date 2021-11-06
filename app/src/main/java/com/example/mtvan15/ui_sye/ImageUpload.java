package com.example.mtvan15.ui_sye;

import android.graphics.Bitmap;

public class ImageUpload {

    private String title;
    private String description;
    private Bitmap bitmap;

    public ImageUpload(String title, String description, Bitmap bitmap){
        this.title = title;
        this.description = description;
        this.bitmap = bitmap;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
