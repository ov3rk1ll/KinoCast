package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.SparseArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class KinoxParser extends Parser{
    public static final int PARSER_ID = 0;
    public static final String URL_BASE = "http://www.kinox.tv/";

    private static final SparseArray<Integer> languageResMap = new SparseArray<Integer>();
    private static final SparseArray<String> languageKeyMap = new SparseArray<String>();
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

    @Override
    public int getParserId(){
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc){
        List<ViewModel> list = new ArrayList<ViewModel>();
        Elements files = doc.select("div.MiniEntry");

        for(Element element : files){
            element = element.parent();
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
            if(genre.contains(",")) genre = genre.substring(0, genre.indexOf(","));
            model.setGenre(genre);

            String rating = element.select("div.Genre > div.floatright").text();
            rating = rating.substring(rating.indexOf(":") + 1, rating.indexOf("/") - 1);
            model.setRating(Float.valueOf(rating.trim()));

            model.setImage(getPageLink(model) + "#language=" + language);

            list.add(model);
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url){
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(Utils.USER_AGENT)
                    .cookie("ListMode", "cover")
                    .timeout(10000)
                    .get();

            return parseList(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ViewModel parseDetail(Document doc, ViewModel item){
        if(doc.select("select#SeasonSelection").size() > 0){
            item.setType(ViewModel.Type.SERIES);
            String rel = doc.select("select#SeasonSelection").attr("rel");
            rel = rel.substring(rel.indexOf("SeriesID=") + "SeriesID=".length());
            item.setSeriesID(Integer.valueOf(rel));
            // Fill seasons and episodes
            Elements seasons = doc.select("select#SeasonSelection > option");
            List <Season> list = new ArrayList<Season>();
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
            List<Host> hostlist = new ArrayList<Host>();
            Elements hosts = doc.select("ul#HosterList").select("li");
            for(Element host: hosts){
                int hosterId = Integer.valueOf(host.id().replace("Hoster_", ""));
                String name = host.select("div.Named").text();
                String count = host.select("div.Data").text();
                count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                int c = Integer.valueOf(count);
                for(int i = 0; i < c; i++){
                    Host h = Host.selectById(hosterId);
                    h.setName(name);
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
            Document doc = Jsoup.connect(URL_BASE + "Stream/" + item.getSlug() + ".html")
                    .userAgent(Utils.USER_AGENT)
                    .cookie("StreamHostMirrorMode", "fixed")
                    .cookie("StreamAutoHideMirrros", "Fixed")
                    .cookie("StreamShowFacebook", "N")
                    .cookie("StreamCommentLimit", "0")
                    .cookie("StreamMirrorMode", "fixed")
                    .timeout(10000)
                    .get();

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
            Document doc = Jsoup.connect(url)
                    .userAgent(Utils.USER_AGENT)
                    .cookie("StreamHostMirrorMode", "fixed")
                    .cookie("StreamAutoHideMirrros", "Fixed")
                    .cookie("StreamShowFacebook", "N")
                    .cookie("StreamCommentLimit", "0")
                    .cookie("StreamMirrorMode", "fixed")
                    .timeout(10000)
                    .get();

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
            Document doc = Jsoup.connect(URL_BASE + url)
                    .userAgent(Utils.USER_AGENT)
                    .timeout(10000)
                    .get();

            List<Host> hostlist = new ArrayList<Host>();
            Elements hosts = doc.select("li");
            for(Element host: hosts){
                int hosterId = Integer.valueOf(host.id().replace("Hoster_", ""));
                String name = host.select("div.Named").text();
                String count = host.select("div.Data").text();
                count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                int c = Integer.valueOf(count);
                for(int i = 0; i < c; i++){
                    Host h = Host.selectById(hosterId);
                    h.setName(name);
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

    private String getMirrorLink(String url){
        try {
            JSONObject json = Utils.readJson(URL_BASE + url);
            Document doc = Jsoup.parse(json.getString("Stream"));
            return doc.select("a").attr("href");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getMirrorLink(ViewModel item, int hoster, int mirror){
        return getMirrorLink("aGET/Mirror/" + item.getSlug() + "&Hoster=" + hoster + "&Mirror=" + mirror);
    }

    @Override
    public String getMirrorLink(ViewModel item, int hoster, int mirror, int season, String episode){
        return getMirrorLink("aGET/Mirror/" + item.getSlug() + "&Hoster=" + hoster + "&Mirror=" + mirror + "&Season=" + season + "&Episode=" + episode);
    }

    @Override
    public String[] getSearchSuggestions(String query){
        String url = URL_BASE + "aGET/Suggestions/?q=" + URLEncoder.encode(query) + "&limit=10&timestamp=" + SystemClock.elapsedRealtime();
        String data = Utils.readUrl(url);
        try {
            byte ptext[] = data.getBytes("ISO-8859-1");
            data = new String(ptext, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String suggestions[] = data.split("\n");
        if(suggestions[0].trim().equals("")) return null;
        return suggestions;
    }

    @Override
    public String getPageLink(ViewModel item){
        return URL_BASE + "Stream/" + item.getSlug() + ".html";
    }

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
