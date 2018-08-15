package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KinoxParser extends Parser{
    public static final int PARSER_ID = 0;

    private static final SparseIntArray languageResMap = new SparseIntArray();
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

    KinoxParser(String url) {
        super(url);
    }

    @Override
    public String getParserName() {
        return "Kinox";
    }

    @Override
    public int getParserId(){
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc){
        List<ViewModel> list = new ArrayList<>();
        Elements files = doc.select("div.MiniEntry");

        for(Element element : files){
            element = element.parent();
            try {
                ViewModel model = new ViewModel();
                String url = element.select("h1").parents().attr("href");
                model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
                model.setTitle(element.select("h1").text());
                model.setSummary(element.select("div.Descriptor").text());

                String ln = element.select("div.Genre > div.floatleft").eq(0).select("img").attr("src");
                ln = ln.substring(ln.lastIndexOf("/") + 1);
                ln = ln.substring(0, ln.indexOf("."));
                int lnId = Integer.valueOf(ln);
                model.setLanguageResId(languageResMap.get(lnId));
                String language = languageKeyMap.get(lnId);

                String genre = element.select("div.Genre > div.floatleft").eq(1).text();
                genre = genre.substring(genre.indexOf(":") + 1).trim();
                if (genre.contains(",")) genre = genre.substring(0, genre.indexOf(","));
                model.setGenre(genre);

                String rating = element.select("div.Genre > div.floatright").text();
                rating = rating.substring(rating.indexOf(":") + 1, rating.indexOf("/") - 1);
                model.setRating(Float.valueOf(rating.trim()));

                model.setImage(getPageLink(model) + "#language=" + language);

                list.add(model);
            }catch (Exception e){
                Log.e("Kinox", "Error parsing " + element.html(), e);
            }
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i("Parser", "parseList: " + url);
        Map<String, String> cookies = new HashMap<>();
        cookies.put("ListMode", "cover");
        Document doc = getDocument(url, cookies);
        return parseList(doc);
    }

    private ViewModel parseDetail(Document doc, ViewModel item){
        if(doc.select("select#SeasonSelection").size() > 0){
            item.setType(ViewModel.Type.SERIES);
            String rel = doc.select("select#SeasonSelection").attr("rel");
            rel = rel.substring(rel.indexOf("SeriesID=") + "SeriesID=".length());
            item.setSeriesID(Integer.valueOf(rel));
            // Fill seasons and episodes
            Elements seasons = doc.select("select#SeasonSelection > option");
            List <Season> list = new ArrayList<>();
            for(Element season : seasons){
                String[] rels = season.attr("rel").split(",");
                Season s = new Season();
                s.id = Integer.valueOf(season.val());
                s.name = season.text();
                s.episodes = rels;
                list.add(s);
            }
            item.setSeasons(list.toArray(new Season[list.size()]));
        }else{
            item.setType(ViewModel.Type.MOVIE);
            List<Host> hostlist = new ArrayList<>();
            Elements hosts = doc.select("ul#HosterList").select("li");
            for(Element host: hosts){
                int hosterId = 0;
                Set<String> classes = host.classNames();
                for (String c : classes) {
                    if(c.startsWith("MirStyle")){
                        hosterId = Integer.valueOf(c.substring("MirStyle".length()));
                    }
                }
                String count = host.select("div.Data").text();
                int c = 1;
                if(count.contains("/")) {
                    count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                    c = Integer.valueOf(count);
                }
                for(int i = 0; i < c; i++){
                    Host h = Host.selectById(hosterId);
                    if(h == null) continue;
                    h.setMirror(i + 1);
                    if(h.isEnabled()){
                        hostlist.add(h);
                    }
                }
            }
            item.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
        }
        String imdb = doc.select("div.IMDBRatingLinks > a").attr("href").trim();
        if(!TextUtils.isEmpty(imdb)){
            imdb = imdb.replace("/", "");
            item.setImdbId(imdb);
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(ViewModel item){
        try {
            Document doc = super.getDocument(URL_BASE + "Stream/" + item.getSlug() + ".html");

            return parseDetail(doc, item);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(String url) {
        ViewModel model = new ViewModel();
        try {
            Map<String, String> cookies = new HashMap<>();
            cookies.put("StreamHostMirrorMode", "fixed");
            cookies.put("StreamAutoHideMirrros", "Fixed");
            cookies.put("StreamShowFacebook", "N");
            cookies.put("StreamCommentLimit", "0");
            cookies.put("StreamMirrorMode", "fixed");
            Document doc = getDocument(url, cookies);

            model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
            model.setTitle(doc.select("h1 > span:eq(0)").text());
            model.setSummary(doc.select("div.Descriptore").text());
            model.setImage(URL_BASE + doc.select("div.Grahpics img").attr("src"));
            String ln = doc.select("div.Flag > img").attr("src");
            ln = ln.substring(ln.lastIndexOf("/") + 1);
            ln = ln.substring(0, ln.indexOf("."));
            int lnId = Integer.valueOf(ln);

            model.setLanguageResId(languageResMap.get(lnId));
            String language = languageKeyMap.get(lnId);

            model.setImage(getPageLink(model) + "#language=" + language);

            model.setGenre(doc.select("li[Title=Genre]").text());

            String rating = doc.select("div.IMDBRatingLabel").text();
            rating = rating.substring(0, rating.indexOf("/"));
            rating = rating.trim();
            model.setRating(Float.valueOf(rating));

            model = parseDetail(doc, model);

            return model;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode){
        String url = "aGET/MirrorByEpisode/?Addr=" + item.getSlug() + "&SeriesID=" + item.getSeriesID() + "&Season=" + season + "&Episode=" + episode;
        try {
            Document doc = getDocument(URL_BASE + url);

            List<Host> hostlist = new ArrayList<>();
            Elements hosts = doc.select("li");
            for(Element host: hosts){
                int hosterId = Integer.valueOf(host.id().replace("Hoster_", ""));
                String count = host.select("div.Data").text();
                count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                int c = Integer.valueOf(count);
                for(int i = 0; i < c; i++){
                    Host h = Host.selectById(hosterId);
                    if(h == null) continue;
                    h.setMirror(i + 1);
                    if(h.isEnabled()){
                        hostlist.add(h);
                    }
                }
            }

            return hostlist;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getMirrorLink(DetailActivity.QueryPlayTask queryTask, Host host, String url){

        String href = "";
        Method getLink = null;

        try {
            getLink = host.getClass().getMethod("getMirrorLink", Document.class);
        } catch (NoSuchMethodException e) {
            //not implemented
            //i'm not a java developer so i didn't want to change much of the code
        }

        try {
            queryTask.updateProgress("Get host from " + URL_BASE + url);
            JSONObject json = getJson(URL_BASE + url);
            Document doc = Jsoup.parse(json != null ? json.getString("Stream") : null);

            if (getLink == null){
                //fallback to old
                href = doc.select("a").attr("href");
            } else {
                href = (String) getLink.invoke(null, doc);
            }

            queryTask.updateProgress("Get video from " + href);
            return href;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host){
        return getMirrorLink(queryTask, host,"aGET/Mirror/" + item.getSlug() + "&Hoster=" + host.getId() + "&Mirror=" + host.getMirror());
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host, int season, String episode){
        return getMirrorLink(queryTask, host,"aGET/Mirror/" + item.getSlug() + "&Hoster=" + host.getId() + "&Mirror=" + host.getMirror() + "&Season=" + season + "&Episode=" + episode);
    }

    @SuppressWarnings("deprecation")
    @Override
    public String[] getSearchSuggestions(String query){
        String url = URL_BASE + "aGET/Suggestions/?q=" + URLEncoder.encode(query) + "&limit=10&timestamp=" + SystemClock.elapsedRealtime();
        String data = getBody(url);
        /*try {
            byte ptext[] = data.getBytes("ISO-8859-1");
            data = new String(ptext, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
        String suggestions[] = data != null ? data.split("\n") : new String[0];
        if(suggestions[0].trim().equals("")) return null;
        // Remove duplicates
        return new HashSet<>(Arrays.asList(suggestions)).toArray(new String[new HashSet<>(Arrays.asList(suggestions)).size()]);
    }

    @Override
    public String getPageLink(ViewModel item){
        return URL_BASE + "Stream/" + item.getSlug() + ".html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query){
        return URL_BASE + "Search.html?q=" + URLEncoder.encode(query);
    }

    @Override
    public String getCineMovies(){
        return URL_BASE + "Cine-Films.html";
    }

    @Override
    public String getPopularMovies(){
        return URL_BASE + "Popular-Movies.html";
    }

    @Override
    public String getLatestMovies(){
        return URL_BASE + "Latest-Movies.html";
    }

    @Override
    public String getPopularSeries(){
        return URL_BASE + "Popular-Series.html";
    }

    @Override
    public String getLatestSeries(){
        return URL_BASE + "Latest-Series.html";
    }
}
