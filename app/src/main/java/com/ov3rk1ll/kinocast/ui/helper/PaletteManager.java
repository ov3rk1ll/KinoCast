package com.ov3rk1ll.kinocast.ui.helper;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.support.v7.graphics.Palette;

public class PaletteManager {
    private LruCache<String, Palette> cache = new LruCache<>(100);
    private static PaletteManager instance;

    public static PaletteManager getInstance(){
        if(instance == null) instance = new PaletteManager();
        return instance;
    }

    public interface Callback {
        void onPaletteReady(Palette palette);
    }

    public void getPalette(final String key, Bitmap bitmap, final Callback callback) {
        Palette palette = cache.get(key);
        if (palette != null)
            callback.onPaletteReady(palette);
        else
            if(bitmap == null){
                callback.onPaletteReady(null);
            } else {
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                  public void onGenerated(Palette p) {
                      cache.put(key, p);
                      callback.onPaletteReady(p);
                  }
                });
            }
    }
}
