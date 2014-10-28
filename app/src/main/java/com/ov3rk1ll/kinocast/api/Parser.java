package com.ov3rk1ll.kinocast.api;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;

import java.util.List;

public class Parser {
    public static final int PARSER_ID = -1;

    private static Parser instance;
    public static Parser getInstance(){
        return instance;
    }

    public static void selectParser(int id){
        instance = selectByParserId(id);
    }
    public static Parser selectByParserId(int id){
        switch (id){
            case KinoxParser.PARSER_ID: return new KinoxParser();
        }
        return new Parser();
    }

    public int getParserId(){
        return PARSER_ID;
    }

    /**
     * Reads ViewModel list from given page
     * @param url
     * @return
     */
    public List<ViewModel> parseList(String url){
        return null;
    }

    public ViewModel loadDetail(ViewModel item){
        return item;
    }

    public ViewModel loadDetail(String url){
        return null;
    }

    public List<Host> getHosterList(ViewModel item, int season, String episode){
        return null;
    }

    private String getMirrorLink(String url){
        return null;
    }

    public String getMirrorLink(ViewModel item, int hoster, int mirror){
        return null;
    }

    public String getMirrorLink(ViewModel item, int hoster, int mirror, int season, String episode){
        return null;
    }

    public String[] getSearchSuggestions(String query){
        return null;
    }

    public String getPageLink(ViewModel item){
        return null;
    }

    public String getSearchPage(String query){
        return null;
    }

    public String getCineMovies(){
        return null;
    }

    public String getPopularMovies(){
        return null;
    }

    public String getLatestMovies(){
        return null;
    }

    public String getPopularSeries(){
        return null;
    }

    public String getLatestSeries(){
        return null;
    }
}
