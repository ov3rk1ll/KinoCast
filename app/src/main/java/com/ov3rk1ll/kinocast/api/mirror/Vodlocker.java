package com.ov3rk1ll.kinocast.api.mirror;

import android.util.Log;

import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sg on 19.09.2016.
 */
public class Vodlocker extends Host {
    private static final String TAG = Vodlocker.class.getSimpleName();
    public static final int HOST_ID = 65;

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "Vodlocker";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        try {
            Log.d(TAG, "GET " + url);
            Document doc = Jsoup.connect(url)
                    .timeout(3000)
                    .get();

            String id = doc.select("form > input[name=id]").val();
            String fname = doc.select("form > input[name=fname]").val();
            String hash = doc.select("form > input[name=hash]").val();

            return getLink(id, fname, hash);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getLink(String id, String fname, String hash){
        try {
            Log.d(TAG, "POST " + url + " w/ " + id + ", " + fname + ", " + hash);
            Document doc = Jsoup.connect(url)
                    .data("op", "download1")
                    .data("id", id)
                    .data("fname", fname)
                    .data("imhuman", "Proceed to video")
                    .data("usr_login", "")
                    .data("referer", "")
                    .data("hash", hash)
                    .userAgent(Utils.USER_AGENT)
                    .timeout(3000)
                    .post();
            Pattern p = Pattern.compile("file: \\\"(.*)\\/v\\.mp4\\\",");
            Matcher m = p.matcher(doc.html());
            if(m.find()){
                return m.group(1) + "/video.mp4";
            } else {
                Log.d(TAG, "file-pattern not found in '" + doc.select("script:contains(jwplayer)").html() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
