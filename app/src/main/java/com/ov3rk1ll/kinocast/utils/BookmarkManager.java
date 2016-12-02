package com.ov3rk1ll.kinocast.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.data.ViewModel;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookmarkManager extends ArrayList<BookmarkManager.Bookmark> {
    public static final String FILENAME = "bookmark.dat";
    private transient OkHttpClient client;

    private transient Context context;
    private transient boolean autoSave = true;

    public BookmarkManager(Context context){
        this.context = context;
        this.client = Utils.buildHttpClient(context);
        restore();
    }

    public void save(){
        try {
            File f = new File(context.getFilesDir(), FILENAME);
            if(f.exists()) f.delete();
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void restore(){
        try {
            FileInputStream fis = context.openFileInput(FILENAME);
            ObjectInputStream is = new ObjectInputStream(fis);
            BookmarkManager simpleClass = (BookmarkManager) is.readObject();
            is.close();
            simpleClass.setAutoSave(false);
            Collections.sort(simpleClass, new Comparator<Bookmark>() {
                @Override
                public int compare(Bookmark s1, Bookmark s2) {
                    return s1.getUrl().compareToIgnoreCase(s2.getUrl());
                }
            });
            clear();
            for(int i = 0; i < simpleClass.size(); i++){
                super.add(simpleClass.get(i));
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public boolean add(Bookmark object) {
        boolean r = super.add(object);
        if(isAutoSave()){
            save();
            pushToServer(object);
        }
        return r;
    }

    @Override
    public boolean remove(Object object) {
        boolean r = super.remove(object);
        if(isAutoSave()) {
            save();
            deleteFromServer((Bookmark) object);
        }
        return r;
    }

    @Override
    public Bookmark set(int index, Bookmark object) {
        Bookmark r = super.set(index, object);
        if(isAutoSave()){
            save();
            pushToServer(object);
        }
        return r;
    }

    public void addAsPublic(Bookmark bookmark) {
        bookmark.setInternal(false);
        int idx = indexOf(bookmark);
        if(idx == -1){
            add(bookmark);
        } else {
            set(idx, bookmark);
        }
        pushToServer(bookmark);
    }

    public void pushToServer(Bookmark bookmark){
        FormBody formBody = new FormBody.Builder()
                .add("slug", bookmark.getSlug())
                .add("parser", bookmark.getParserName())
                .add("url", bookmark.getUrl())
                .add("internal", String.valueOf(bookmark.isInternal()))
                .add("season", String.valueOf(bookmark.getSeason()))
                .add("episode", String.valueOf(bookmark.getEpisode()))
                .build();
        for(int i = 0; i < formBody.size(); i++){
            Log.i("pushToServer", formBody.encodedName(i) + "=" + formBody.encodedValue(i));
        }
        Request request = new Request.Builder()
                .url(context.getString(R.string.api_server) + "/api/users/me/favorites")
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                }

                System.out.println(response.body().string());
            }
        });
    }

    public void deleteFromServer(Bookmark bookmark){
        Request request = new Request.Builder()
                .url(context.getString(R.string.api_server) + "/api/users/me/favorites/" + bookmark.getSlug())
                .delete()
                .build();
        Log.i("deleteFromServer", "Do " + request);

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                }

                System.out.println(response.body().string());
            }
        });
    }

    public Bookmark findItem(ViewModel item){
        Bookmark b = new BookmarkManager.Bookmark(
                Parser.getInstance().getParserName(),
                item.getSlug(),
                Parser.getInstance().getPageLink(item)
        );
        int idx = indexOf(b);
        if(idx == -1){
            return null;
        } else {
            return get(idx);
        }
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public static class Bookmark implements Serializable{
        private String parserName;
        private String slug;
        private String url;
        private int season = 0;
        private int episode = 0;
        private boolean internal = true;

        public Bookmark() {

        }

        public Bookmark(JSONObject json) throws Exception{
            parserName = json.getString("parser");
            slug = json.getString("slug");
            url = json.getString("url");
            internal = json.getBoolean("internal");
            if(json.has("season")) season = json.getInt("season");
            if(json.has("episode")) episode = json.getInt("episode");
        }

        public Bookmark(String parserName, String slug, String url) {
            this.parserName = parserName;
            this.slug = slug;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getEpisode() {
            return episode;
        }

        public void setEpisode(int episode) {
            this.episode = episode;
        }

        public int getSeason() {
            return season;
        }

        public void setSeason(int season) {
            this.season = season;
        }

        public boolean isInternal() {
            return internal;
        }

        public void setInternal(boolean internal) {
            this.internal = internal;
        }

        public String getParserName() {
            return parserName;
        }

        public String getSlug() {
            return slug;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Bookmark){
                Bookmark b = (Bookmark)o;
                return TextUtils.equals(b.getParserName(), this.getParserName()) && TextUtils.equals(b.getUrl(), this.getUrl());
            }
            return super.equals(o);
        }
    }
}
