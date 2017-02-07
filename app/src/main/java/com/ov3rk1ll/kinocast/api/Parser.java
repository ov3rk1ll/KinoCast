package com.ov3rk1ll.kinocast.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import java.util.List;

public abstract class Parser {
    public static final int PARSER_ID = -1;
    protected String URL_BASE; // = "http://www.kinox.sg/";

    private static Parser instance;
    public static Parser getInstance(){
        return instance;
    }

    public static void selectParser(Context context, int id){
        instance = selectByParserId(context, id);
    }
    public static void selectParser(Context context, int id, String url){
        instance = selectByParserId(context, id, url);
    }
    public static Parser selectByParserId(Context context, int id){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String url = preferences.getString("url", "http://www.kinox.sg");
        return selectByParserId(context, id, url);
    }
    private static Parser selectByParserId(Context context, int id, String url) {
        if (!url.endsWith("/")) url = url + "/";
        Log.i("Parser", "selectByParserId: load with #" + id + " for " + url);
        switch (id) {
            case KinoxParser.PARSER_ID:
                return new KinoxParser(url);
            case Movie4kParser.PARSER_ID:
                return new Movie4kParser(url);
        }
        return null;
    }


    public Parser(String url) {
        this.URL_BASE = url;
    }

    public abstract String getParserName();

    public int getParserId(){
        return PARSER_ID;
    }

    public abstract List<ViewModel> parseList(String url);

    public abstract ViewModel loadDetail(ViewModel item);

    public abstract ViewModel loadDetail(String url);

    public abstract List<Host> getHosterList(ViewModel item, int season, String episode);

    public abstract String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, int hoster, int mirror);

    public abstract String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, int hoster, int mirror, int season, String episode);

    public abstract String[] getSearchSuggestions(String query);

    public abstract String getPageLink(ViewModel item);

    public abstract String getSearchPage(String query);

    public abstract String getCineMovies();

    public abstract String getPopularMovies();

    public abstract String getLatestMovies();

    public abstract String getPopularSeries();

    public abstract String getLatestSeries();

    public String getUrl(){
        return URL_BASE;
    }

    @Override
    public String toString() {
        return getParserName();
    }
}
