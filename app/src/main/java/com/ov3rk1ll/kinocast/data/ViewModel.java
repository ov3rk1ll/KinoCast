package com.ov3rk1ll.kinocast.data;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.ui.helper.smartimageview.CoverImage;

import java.io.Serializable;

public class ViewModel implements Serializable {

    private int seriesID;
    private String slug;
    private String title;
    private String image;
    private String backdrop;
    private String summary;
    private float rating;
    private int languageResId;
    private String genre;
    private String imdbId;
    private String year;

    private Type type;

    private transient Season[] seasons;
    private transient Host[] mirrors;

    public ViewModel() {

    }

    public int getSeriesID() {
        return seriesID;
    }

    public void setSeriesID(int seriesID) {
        this.seriesID = seriesID;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImage() {
        return this.image;
    }

    public String getImage(int targetWidth, String type) {
        return this.image + "&size=" + targetWidth + "&type=" + type;
    }

    public CoverImage.Request getImageRequest(int targetWidth, String type) {
        return new CoverImage.Request(this, targetWidth, type);
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getLanguageResId() {
        return languageResId;
    }

    public void setLanguageResId(int languageResId) {
        this.languageResId = languageResId;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Season[] getSeasons() {
        return seasons;
    }

    public void setSeasons(Season[] seasons) {
        this.seasons = seasons;
    }

    public Host[] getMirrors() {
        return mirrors;
    }

    public void setMirrors(Host[] mirrors) {
        this.mirrors = mirrors;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public enum Type{
        MOVIE,
        SERIES,
        DOCUMENTATION
    }
}
