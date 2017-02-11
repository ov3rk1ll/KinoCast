package com.ov3rk1ll.kinocast.utils;

import android.content.Context;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.data.ViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TheMovieDb {
    public static final String IMAGE_BASE_PATH = "http://image.tmdb.org/t/p/";
    private static final String DISK_CACHE_PATH = "/themoviedb_cache/";
    private static final String API_KEY = "f9dc7e5d12b2640bf4ef1cf20835a1cc";

    private ConcurrentHashMap<String, SoftReference<JSONObject>> memoryCache;
    private String diskCachePath;
    private boolean diskCacheEnabled = false;
    private ExecutorService writeThread;


    public TheMovieDb(Context context) {
        // Set up in-memory cache store
        memoryCache = new ConcurrentHashMap<>();

        // Set up disk cache store
        Context appContext = context.getApplicationContext();
        diskCachePath = appContext.getCacheDir().getAbsolutePath() + DISK_CACHE_PATH;

        File outFile = new File(diskCachePath);
        outFile.mkdirs();

        diskCacheEnabled = outFile.exists();

        // Set up threadpool for image fetching tasks
        writeThread = Executors.newSingleThreadExecutor();
    }

    public JSONObject get(String url) {
        return get(url, true);
    }

    public JSONObject get(String url, boolean fetchIfNeeded) {
        JSONObject json;
        //String url = request.getUrl();

        // Check for image in memory
        json = getFromMemory(url);

        // Check for image on disk cache
        if(json == null) {
            json = getFromDisk(url);

            // Write bitmap back into memory cache
            if(json != null) {
                cacheToMemory(url, json);
            }
        }
        if(json == null || fetchIfNeeded) {
            try {
                // Get IMDB-ID from page
                ViewModel item = Parser.getInstance().loadDetail(url);
                String param = url.substring(url.indexOf("#") + 1);
                // tt1646971?api_key=f9dc7e5d12b2640bf4ef1cf20835a1cc&language=de&external_source=imdb_id
                JSONObject data = Utils.readJson("http://api.themoviedb.org/3/find/" + item.getImdbId() + "?api_key=" + API_KEY + "&external_source=imdb_id&" + param);
                if(data == null) return null;
                if(data.getJSONArray("movie_results").length() > 0) {
                    json = data.getJSONArray("movie_results").getJSONObject(0);
                }else if(data.getJSONArray("tv_results").length() > 0) {
                    json = data.getJSONArray("tv_results").getJSONObject(0);
                }
                if(json != null) {
                    put(url, json);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    private void put(String url, JSONObject json) {
        cacheToMemory(url, json);
        cacheToDisk(url, json);
    }

    private JSONObject getFromMemory(String url) {
        JSONObject json = null;
        SoftReference<JSONObject> softRef = memoryCache.get(getCacheKey(url));
        if(softRef != null){
            json = softRef.get();
        }

        return json;
    }

    private JSONObject getFromDisk(String url) {
        JSONObject json = null;
        if(diskCacheEnabled){
            String filePath = getFilePath(url);
            File file = new File(filePath);
            if(file.exists()) {
                try {
                    BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    StringBuilder content = new StringBuilder();
                    char[] buffer = new char[1024];
                    int num;
                    while ((num = input.read(buffer)) > 0) {
                        content.append(buffer, 0, num);
                    }
                    json = new JSONObject(content.toString());
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return json;
    }

    private void cacheToMemory(final String url, final JSONObject json) {
        memoryCache.put(getCacheKey(url), new SoftReference<>(json));
    }

    private void cacheToDisk(final String url, final JSONObject json) {
        writeThread.execute(new Runnable() {
            @Override
            public void run() {
                if(diskCacheEnabled) {
                    FileOutputStream ostream = null;
                    try {
                        ostream = new FileOutputStream(new File(diskCachePath, getCacheKey(url)));
                        ostream.write(json.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if(ostream != null) {
                                ostream.flush();
                                ostream.close();
                            }
                        } catch (IOException ignored) {}
                    }
                }
            }
        });
    }

    private String getFilePath(String url) {
        return diskCachePath + getCacheKey(url);
    }

    private String getCacheKey(String url) {
        if(url == null){
            throw new RuntimeException("Null url passed in");
        } else {
            if(url.contains("#"))
                url = url.substring(0, url.indexOf("#"));
            return url.replaceAll("[.:/,%?&=]", "+").replaceAll("[+]+", "+");
        }
    }
}