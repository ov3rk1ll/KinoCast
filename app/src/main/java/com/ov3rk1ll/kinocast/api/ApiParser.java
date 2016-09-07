package com.ov3rk1ll.kinocast.api;

import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApiParser extends Parser {
    public static final String TAG = "ApiParser";
    public static final String URL_BASE = "http://floating-dusk-21938.herokuapp.com/api/";
    public static final String API_KEY = "ba9a68c9c1687fab08becee55d148af4";

    private static final SparseArray<Integer> languageResMap = new SparseArray<>();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();
    static {
        languageResMap.put(1, R.drawable.lang_de); languageKeyMap.put(1, "de");
        languageResMap.put(2, R.drawable.lang_en); languageKeyMap.put(2, "de");
        languageResMap.put(4, R.drawable.lang_zh); languageKeyMap.put(4, "zh");
        languageResMap.put(5, R.drawable.lang_es); languageKeyMap.put(5, "es");
        languageResMap.put(6, R.drawable.lang_fr); languageKeyMap.put(6, "fr");
        languageResMap.put(7, R.drawable.lang_tr); languageKeyMap.put(7, "tr");
        languageResMap.put(8, R.drawable.lang_jp); languageKeyMap.put(8, "jp");
        languageResMap.put(9, R.drawable.lang_ar); languageKeyMap.put(9, "ar");
        languageResMap.put(11, R.drawable.lang_it); languageKeyMap.put(11, "it");
        languageResMap.put(12, R.drawable.lang_hr); languageKeyMap.put(12, "hr");
        languageResMap.put(13, R.drawable.lang_sr); languageKeyMap.put(13, "sr");
        languageResMap.put(14, R.drawable.lang_bs); languageKeyMap.put(14, "bs");
        languageResMap.put(15, R.drawable.lang_de_en); languageKeyMap.put(15, "en");
        languageResMap.put(16, R.drawable.lang_nl); languageKeyMap.put(16, "nl");
        languageResMap.put(17, R.drawable.lang_ko); languageKeyMap.put(17, "ko");
        languageResMap.put(24, R.drawable.lang_el); languageKeyMap.put(24, "el");
        languageResMap.put(25, R.drawable.lang_ru); languageKeyMap.put(25, "ru");
        languageResMap.put(26, R.drawable.lang_hi); languageKeyMap.put(26, "hi");
    }

    private final OkHttpClient client = new OkHttpClient();
    private String mName;

    public ApiParser(String name){
        this.mName = name;
    }

    @Override
    public String getParserName() {
        return "ApiParser";
    }

    private ViewModel buildModelFromJson(JSONObject entry) throws JSONException {
        ViewModel model = new ViewModel();
        model.setSlug(entry.getString("slug"));
        model.setTitle(entry.getString("title"));
        model.setSummary(entry.getString("summary"));
        model.setLanguageResId(languageResMap.get(entry.getInt("language")));
        model.setGenre(entry.getString("genre"));
        model.setRating((float) entry.getDouble("rating"));

        return model;
    }

    @Override
    public List<ViewModel> parseList(String url) {
        Log.i(TAG, "GET " + url);
        List<ViewModel> list = new ArrayList<>();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .build();

        try {
            Response response = client.newCall(request).execute();
            JSONArray entries = new JSONArray(response.body().string());
            for (int i = 0; i < entries.length(); i++){
                ViewModel model = buildModelFromJson(entries.getJSONObject(i));
                list.add(model);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private ViewModel parseDetail(JSONObject data) throws JSONException {
        ViewModel item = buildModelFromJson(data);
        if (TextUtils.equals("SERIES", data.getString("type"))) {
            item.setType(ViewModel.Type.SERIES);
            JSONArray seasons = data.getJSONArray("seasons");

            List<Season> list = new ArrayList<>();
            for (int i = 0; i < seasons.length(); i++) {
                JSONObject season = seasons.getJSONObject(i);
                Season s = new Season();
                s.id = season.getInt("id");
                s.name = season.getString("name");
                JSONArray episodes = season.getJSONArray("episodes");
                s.episodes = new String[episodes.length()];
                for (int j = 0; j < s.episodes.length; j++) {
                    s.episodes[j] = episodes.getString(j);
                }
                list.add(s);
            }
            item.setSeasons(list.toArray(new Season[list.size()]));
        } else if (TextUtils.equals("MOVIE", data.getString("type"))) {
            item.setType(ViewModel.Type.MOVIE);
            List<Host> hostlist = new ArrayList<>();
            JSONArray hosts = data.getJSONArray("mirrors");
            for (int i = 0; i < hosts.length(); i++) {
                JSONObject host = hosts.getJSONObject(i);
                Host h = Host.selectById(host.getInt("id"));
                h.setName(host.getString("name"));
                h.setMirror(host.getInt("mirror"));
                if (h.isEnabled()) {
                    hostlist.add(h);
                }
            }
            item.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(ViewModel item) {
        String url = URL_BASE + mName + "/movies/" + item.getSlug();
        return loadDetail(url);
    }

    @Override
    public ViewModel loadDetail(String url) {
        Log.i(TAG, "GET " + url);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .build();

        try {
            Response response = client.newCall(request).execute();
            JSONObject data = new JSONObject(response.body().string());
            return parseDetail(data);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        String url = URL_BASE + mName + "/movies/" + item.getSlug() + "/getEpisodeHosts?season=" + season + "&episode=" + episode;
        Log.i(TAG, "GET " + url);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .build();
        List<Host> hostlist = new ArrayList<>();

        try {
            Response response = client.newCall(request).execute();
            JSONArray hosts = new JSONArray(response.body().string());
            for (int i = 0; i < hosts.length(); i++) {
                JSONObject host = hosts.getJSONObject(i);
                Host h = Host.selectById(host.getInt("id"));
                h.setName(host.getString("name"));
                h.setMirror(host.getInt("mirror"));
                if (h.isEnabled()) {
                    hostlist.add(h);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return hostlist;
    }

    private String getMirrorLink(DetailActivity.QueryPlayTask queryTask, String url){
        try {
            Log.i(TAG, "GET " + url);
            queryTask.updateProgress("Get host...");
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", API_KEY)
                    .build();
            Response response = client.newCall(request).execute();
            JSONObject json = new JSONObject(response.body().string());
            String href = json.getString("Stream");
            queryTask.updateProgress("Get video from " + href);
            return href;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, int hoster, int mirror) {
        String url = URL_BASE + mName + "/movies/" + item.getSlug() + "/getHost?id=" + hoster + "&mirror=" + mirror;
        return getMirrorLink(queryTask, url);
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, int hoster, int mirror, int season, String episode) {
        String url = URL_BASE + mName + "/movies/" + item.getSlug() + "/getHost?id=" + hoster + "&mirror=" + "&season=" + season + "&episode=" + episode;
        return getMirrorLink(queryTask, url);
    }

    @Override
    public String[] getSearchSuggestions(String query) {
        return new String[0];
    }

    @Override
    public String getPageLink(ViewModel item) {
        return null;
    }

    @Override
    public String getSearchPage(String query) {
        return null;
    }

    @Override
    public String getCineMovies(){
        return URL_BASE + mName + "/movies/list/" + "Cine-Films";
    }

    @Override
    public String getPopularMovies(){
        return URL_BASE + mName + "/movies/list/" + "Popular-Movies";
    }

    @Override
    public String getLatestMovies(){
        return URL_BASE + mName + "/movies/list/" + "Latest-Movies";
    }

    @Override
    public String getPopularSeries(){
        return URL_BASE + mName + "/movies/list/" + "Popular-Series";
    }

    @Override
    public String getLatestSeries(){
        return URL_BASE + mName + "/movies/list/" + "Latest-Series";
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

    @Override
    public String getImageUrl(ViewModel item, int targetWidth, String type) {
        if(type.equals("backdrop"))
            return URL_BASE + mName + "/movies/" + item.getSlug() + "/image/" + getBackdropSize(targetWidth) + "/backdrop.jpg";
        else
            return URL_BASE + mName + "/movies/" + item.getSlug() + "/image/" + getPosterSize(targetWidth) + "/poster.jpg";

    }
}
