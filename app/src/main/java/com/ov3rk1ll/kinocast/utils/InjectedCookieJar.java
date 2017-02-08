package com.ov3rk1ll.kinocast.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class InjectedCookieJar implements CookieJar {
    private final List<Cookie> cookieStore = new ArrayList<>();

    @Override
    public String toString() {
        return Arrays.toString(cookieStore.toArray());
    }

    public void addCookie(Cookie cookie){
        // Check if we have that cookie so we can overwrite it
        for(int i = 0; i < cookieStore.size(); i++){
            if(cookieStore.get(i).name().equals(cookie.name())){
                cookieStore.remove(i);
                break;
            }
        }
        cookieStore.add(cookie);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cookieStore.addAll(cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return cookieStore;
    }
}
