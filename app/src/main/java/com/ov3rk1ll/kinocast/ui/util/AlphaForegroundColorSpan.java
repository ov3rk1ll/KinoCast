package com.ov3rk1ll.kinocast.ui.util;

import android.graphics.Color;
import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

public class AlphaForegroundColorSpan extends ForegroundColorSpan {

    private int mAlpha;

    public AlphaForegroundColorSpan(int color) {
        super(color);
    }

    public AlphaForegroundColorSpan(Parcel src) {
        super(src);
        mAlpha = src.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mAlpha);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(getAlphaColor());
    }

    public void setAlpha(int alpha) {
        mAlpha = alpha;
    }

    public int getAlpha() {
        return mAlpha;
    }

    private int getAlphaColor() {
        int foregroundColor = getForegroundColor();
        return Color.argb(mAlpha, Color.red(foregroundColor), Color.green(foregroundColor), Color.blue(foregroundColor));
    }
}
