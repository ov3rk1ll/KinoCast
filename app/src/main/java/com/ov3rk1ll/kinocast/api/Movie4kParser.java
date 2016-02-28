package com.ov3rk1ll.kinocast.api;

import android.util.SparseArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Movie4kParser extends Parser{
    public static final int PARSER_ID = 1;
    public static final String URL_BASE = "http://www.movie4k.tv/";

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
    public String getParserName() {
        return "Movie4k";
    }

    @Override
    public int getParserId(){
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc){
        List<ViewModel> list = new ArrayList<ViewModel>();
        Elements files = doc.select("div#maincontentnew");
        for(Element file : files){
            ViewModel model = new ViewModel();

            Elements divs = file.select("div");
            Element data = divs.get(2);
            model.setSlug(data.select("h2 > a").attr("href"));
            model.setTitle(data.select("h2 > a").text());

            Element more = file.select("div.beschreibung").first();
        }
        //TODO
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url){
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(Utils.USER_AGENT)
                    .timeout(10000)
                    .get();

            return parseList(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ViewModel parseDetail(Document doc, ViewModel item){
        // TODO
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
        //TODO
        return null;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode){
        //TODO
        return null;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, int id, int mirror, int i, String url){
        //TODO
        return null;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryPlayTask, ViewModel item, int hoster, int mirror){
        //TODO
        return null;
    }

    @Override
    public String[] getSearchSuggestions(String query){
        //TODO
        return null;
    }

    @Override
    public String getPageLink(ViewModel item){
        //TODO
        return null;
    }

    @Override
    public String getSearchPage(String query){
        //TODO
        return null;
    }

    @Override
    public String getCineMovies(){
        return URL_BASE + "index.php";
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
