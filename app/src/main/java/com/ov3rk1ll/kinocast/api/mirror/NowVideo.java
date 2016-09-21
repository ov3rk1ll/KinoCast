package com.ov3rk1ll.kinocast.api.mirror;

import android.text.TextUtils;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class NowVideo extends Host {
    private static final String TAG = NowVideo.class.getSimpleName();
    public static final int HOST_ID = 40;

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "NowVideo";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        if(TextUtils.isEmpty(url)) return null;
        try {
            String id = url.substring(url.lastIndexOf("/") + 1);
            queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getvideoforid, id));
            Document doc = Jsoup.connect("http://www.nowvideo.sx/mobile/video.php?id=" + id)
                    .userAgent(Utils.USER_AGENT)
                    .timeout(3000)
                    .get();

            return doc.select("source[type=video/mp4]").attr("src");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
