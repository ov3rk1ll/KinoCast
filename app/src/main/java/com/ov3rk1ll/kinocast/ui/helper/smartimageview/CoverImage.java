package com.ov3rk1ll.kinocast.ui.helper.smartimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class CoverImage implements SmartImage {

    public static class Request{
        private ViewModel item;
        private int size;
        private String type;

        public Request(ViewModel item, int size, String type) {
            this.item = item;
            this.size = size;
            this.type = type;
        }

        public ViewModel getItem() {
            return item;
        }

        public void setItem(ViewModel item) {
            this.item = item;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl(){
            return getItem().getImage(getSize(), getType());
        }
    }

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    private static WebImageCache webImageCache;
    private static TheMovieDb tmdbCache;

    private String cacheKey;
    private Request request;

    public CoverImage(Request request) {
        this.request = request;
        this.cacheKey = request.getUrl();
    }

    private String getBackdropSize(int width){
        String size = "w300";
        if(width > 300) size = "w780";
        if(width > 780) size = "w1280";
        if(width > 1280) size = "original";
        return size;
    }

    private String getPosterSize(int width){
        String size = "w92";
        if(width > 92) size = "w154";
        if(width > 154) size = "w185";
        if(width > 185) size = "w342";
        if(width > 342) size = "w500";
        if(width > 500) size = "w780";
        if(width > 780) size = "original";
        return size;
    }

    public Bitmap getBitmap(Context context) {
        // Don't leak context
        if(webImageCache == null) {
            webImageCache = new WebImageCache(context);
        }
        if(tmdbCache == null) {
            tmdbCache = new TheMovieDb(context);
        }

        // Try getting bitmap from cache first
        Bitmap bitmap = null;
        if(cacheKey != null) {
            bitmap = webImageCache.get(cacheKey);
            if(bitmap == null) {
                bitmap = getBitmapFromUrl(cacheKey);
                if(bitmap != null){
                    webImageCache.put(cacheKey, bitmap);
                }
            }
        }

        return bitmap;
    }

    public String getBitmapUrl(Context context) {
        // Don't leak context
        if(webImageCache == null) {
            webImageCache = new WebImageCache(context);
        }
        if(tmdbCache == null) {
            tmdbCache = new TheMovieDb(context);
        }

        return getBitmapUrl(cacheKey);
    }

    private String getBitmapUrl(String url){
        String imageUrl = null;
        try {
            Map<String, List<String>> para = Utils.splitHashQuery(new URL(url));
            JSONObject json = tmdbCache.get(this.request);
            if(json != null){
                String type = para.get("type").get(0);
                String key = type + "_path";
                int size = Integer.valueOf(para.get("size").get(0));
                if(type.equals("backdrop"))
                    imageUrl = TheMovieDb.IMAGE_BASE_PATH + getBackdropSize(size) + json.getString(key);
                else
                    imageUrl = TheMovieDb.IMAGE_BASE_PATH + getPosterSize(size) + json.getString(key);

            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return imageUrl;
    }

    private Bitmap getBitmapFromUrl(String url) {
        Bitmap bitmap = null;

        try {
            String imageUrl = getBitmapUrl(url);
            Log.i("CoverImage", "Load image from " + imageUrl);
            URLConnection conn = new URL(imageUrl).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            bitmap = BitmapFactory.decodeStream((InputStream) conn.getContent());
        } catch(Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static void removeFromCache(String url) {
        if(webImageCache != null) {
            webImageCache.remove(url);
        }
    }
}
