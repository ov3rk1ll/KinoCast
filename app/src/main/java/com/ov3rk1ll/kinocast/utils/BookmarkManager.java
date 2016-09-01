package com.ov3rk1ll.kinocast.utils;

import android.content.Context;
import android.text.TextUtils;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.data.ViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BookmarkManager extends ArrayList<BookmarkManager.Bookmark> {
    public static final String FILENAME = "bookmark.dat";

    private transient Context context;
    private transient boolean autoSave = true;

    public BookmarkManager(Context context){
        this.context = context;
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
        if(isAutoSave()) save();
        return r;
    }

    @Override
    public boolean remove(Object object) {
        boolean r = super.remove(object);
        if(isAutoSave()) save();
        return r;
    }

    @Override
    public Bookmark set(int index, Bookmark object) {
        Bookmark r = super.set(index, object);
        if(isAutoSave()) save();
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
    }

    public Bookmark findItem(ViewModel item){
        Bookmark b = new BookmarkManager.Bookmark(Parser.getInstance().getParserId(), Parser.getInstance().getPageLink(item));
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
        private int parserId;
        private String url;
        private int season = 0;
        private int episode = 0;
        private boolean internal = true;

        public Bookmark() {
        }

        public Bookmark(int parserId, String url) {
            this.parserId = parserId;
            this.url = url;
        }

        public Bookmark(int parserId, String url, boolean internal) {
            this.parserId = parserId;
            this.url = url;
            this.internal = internal;
        }

        public int getParserId() {
            return parserId;
        }

        public void setParserId(int parserId) {
            this.parserId = parserId;
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

        @Override
        public boolean equals(Object o) {
            if(o instanceof Bookmark){
                Bookmark b = (Bookmark)o;
                return b.getParserId() == this.getParserId() && TextUtils.equals(b.getUrl(), this.getUrl());
            }
            return super.equals(o);
        }
    }
}
