package com.ov3rk1ll.kinocast.api.mirror;

import android.util.Log;

import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class DivxStage extends Host {
    private static final String TAG = DivxStage.class.getSimpleName();
    public static final int HOST_ID = 8;

    public DivxStage(int id) {
        super(id);
    }

    @Override
    public String getVideoPath() {
        try {
            url = url.replace("/Out/?s=", "");
            Log.d(TAG, "Resolve " + url);
            String videoId = url.substring(url.lastIndexOf("/") + 1);
            Log.d(TAG, "API call to " + "http://www.divxstage.to/mobile/ajax.php?videoId=" + videoId);
            JSONObject json = Utils.readJson("http://www.divxstage.to/mobile/ajax.php?videoId=" + videoId);
            return Utils.getRedirectTarget("http://www.divxstage.to/mobile/" + json.getJSONArray("items").getJSONObject(0).getString("download"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
