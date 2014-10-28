package com.ov3rk1ll.kinocast.ui.helper.smartimageview;

import android.content.Context;
import android.graphics.Bitmap;

public class BitmapImage implements SmartImage {
    private Bitmap bitmap;

    public BitmapImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap(Context context) {
        return bitmap;
    }
}