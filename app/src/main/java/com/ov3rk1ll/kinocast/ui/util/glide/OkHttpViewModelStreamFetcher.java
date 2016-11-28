package com.ov3rk1ll.kinocast.ui.util.glide;

import android.text.TextUtils;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches an {@link InputStream} using the okhttp3library.
 */
public class OkHttpViewModelStreamFetcher implements DataFetcher<InputStream> {
    private TheMovieDb tmdbCache;

    private final Call.Factory client;
    private final ViewModelGlideRequest model;
    private InputStream stream;
    private ResponseBody responseBody;
    private volatile Call call;

    public OkHttpViewModelStreamFetcher(Call.Factory client, ViewModelGlideRequest model, TheMovieDb tmdbCache) {
        this.client = client;
        this.model = model;
        this.tmdbCache = tmdbCache;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        GlideUrl url;
        String cacheUrl = Parser.getInstance().getPageLink(model.getViewModel()) + "?size=" + model.getScreenWidthPx() + "&type=" + model.getType();

        String imageUrl = model.getViewModel().getImageBase();
        if(TextUtils.isEmpty(imageUrl)) { // No URL - Try via IMDB
            try {
                JSONObject json = tmdbCache.get(model.getViewModel(), cacheUrl);
                if (json != null) {
                    String type = model.getType();
                    String key = type + "_path";
                    int size = model.getScreenWidthPx();
                    if (type.equals("backdrop"))
                        imageUrl = TheMovieDb.IMAGE_BASE_PATH + getBackdropSize(size) + json.getString(key);
                    else
                        imageUrl = TheMovieDb.IMAGE_BASE_PATH + getPosterSize(size) + json.getString(key);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // The server should return a Image url on a know path
            if (model.getType().equals("backdrop")){
                imageUrl = imageUrl + "/" + getBackdropSize(model.getScreenWidthPx()) + "/backdrop.jpg";
            } else {
                imageUrl = imageUrl + "/" + getPosterSize(model.getScreenWidthPx()) + "/poster.jpg";
            }
        }
        url = new GlideUrl(imageUrl);

        Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl());

        for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
            String key = headerEntry.getKey();
            requestBuilder.addHeader(key, headerEntry.getValue());
        }
        Request request = requestBuilder.build();

        Response response;
        call = client.newCall(request);
        response = call.execute();
        responseBody = response.body();
        if (!response.isSuccessful()) {
            throw new IOException("Request failed with code: " + response.code());
        }

        long contentLength = responseBody.contentLength();
        stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
        return stream;
    }

    @Override
    public void cleanup() {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            // Ignored
        }
        if (responseBody != null) {
            responseBody.close();
        }
    }

    @Override
    public String getId() {
        return model.getViewModel().getSlug();
    }

    @Override
    public void cancel() {
        Call local = call;
        if (local != null) {
            local.cancel();
        }
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
}
