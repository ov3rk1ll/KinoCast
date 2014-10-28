package com.ov3rk1ll.kinocast.utils;

import android.content.Context;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.player.VideoCastControllerActivity;

public class CastHelper {
    private static final String APPLICATION_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
    private static VideoCastManager mCastMgr;

    public static VideoCastManager getVideoCastManager(Context ctx) {
        if (null == mCastMgr) {
            mCastMgr = VideoCastManager.initialize(ctx, APPLICATION_ID, VideoCastControllerActivity.class, null);
            mCastMgr.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                    VideoCastManager.FEATURE_LOCKSCREEN |
                    VideoCastManager.FEATURE_WIFI_RECONNECT |
                    VideoCastManager.FEATURE_CAPTIONS_PREFERENCE |
                    VideoCastManager.FEATURE_DEBUGGING);
        }
        mCastMgr.setContext(ctx);
        return mCastMgr;
    }

}
