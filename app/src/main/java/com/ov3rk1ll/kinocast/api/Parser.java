package com.ov3rk1ll.kinocast.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.CloudflareDdosInterceptor;
import com.ov3rk1ll.kinocast.utils.CustomDns;
import com.ov3rk1ll.kinocast.utils.InjectedCookieJar;
import com.ov3rk1ll.kinocast.utils.UserAgentInterceptor;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class Parser {
    private static final String TAG = Parser.class.getSimpleName();
    private static final int PARSER_ID = -1;
    String URL_BASE;
    private static  OkHttpClient client;
    private static InjectedCookieJar injectedCookieJar;

    private static Parser instance;
    public static Parser getInstance(){
        return instance;
    }

    public static void selectParser(Context context, int id){
        instance = selectByParserId(context, id);
        initHttpClient(context);
    }
    public static void selectParser(Context context, int id, String url){
        instance = selectByParserId(id, url);
        initHttpClient(context);
    }
    public static Parser selectByParserId(Context context, int id){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String url = preferences.getString("url", "http://www.kinox.sg");
        return selectByParserId(id, url);
    }
    private static Parser selectByParserId(int id, String url) {
        if (!url.endsWith("/")) url = url + "/";
        Log.i(TAG, "selectByParserId: load with #" + id + " for " + url);
        switch (id) {
            case KinoxParser.PARSER_ID:
                return new KinoxParser(url);
            case Movie4kParser.PARSER_ID:
                return new Movie4kParser(url);
        }
        return null;
    }

    private static void initHttpClient(Context context){
        injectedCookieJar = new InjectedCookieJar();
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .addNetworkInterceptor(new UserAgentInterceptor(Utils.USER_AGENT))
                .addInterceptor(new CloudflareDdosInterceptor(context))
                .cookieJar(injectedCookieJar)
                .dns(new CustomDns())
                .build();
    }

    Document getDocument(String url) throws IOException {
        return getDocument(url, null);
    }

    Document getDocument(String url, Map<String, String> cookies) throws  IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        if(cookies != null) {
            for (String key : cookies.keySet()) {
                injectedCookieJar.addCookie(new Cookie.Builder()
                        .domain(request.url().host())
                        .name(key)
                        .value(cookies.get(key))
                        .build()
                );
            }
        }
        Response response = client.newCall(request).execute();
        if(response.code() != 200){
            throw new IOException("Unexpected status code " + response.code());
        }
        String body = response.body().string();
        if(TextUtils.isEmpty(body)){
            throw new IOException("Body for " + url + " is empty");
        }
        return Jsoup.parse(body);
    }

    public String getBody(String url) {
        OkHttpClient noFollowClient = client.newBuilder().followRedirects(false).build();
        Request request = new Request.Builder().url(url).build();
        Log.i(TAG, "read text from " + url + ", cookies=" + noFollowClient.cookieJar().toString());
        try {
            Response response = noFollowClient.newCall(request).execute();
            Log.i(TAG, "Got " + response.code() + " for " + url + ", cookies=" + noFollowClient.cookieJar().toString());
            for(String key : response.headers().names()){
                Log.i(TAG, key + "=" + response.header(key));
            }

            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    JSONObject getJson(String url) {
        Request request = new Request.Builder().url(url).build();

        Log.i("Utils", "read json from " + url);
        try {
            Response response = client.newCall(request).execute();
            return new JSONObject(response.body().string());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
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

    public abstract List<ViewModel> parseList(String url) throws IOException;

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

    public OkHttpClient getClient(){
        return client;
    }

}
