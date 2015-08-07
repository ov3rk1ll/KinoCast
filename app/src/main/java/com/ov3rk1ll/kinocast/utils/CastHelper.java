package com.ov3rk1ll.kinocast.utils;

import android.content.Context;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

public class CastHelper {
    private static final String APPLICATION_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
    private static VideoCastManager mCastMgr;

    public static VideoCastManager getVideoCastManager(Context ctx) {
        if (null == mCastMgr) {
            VideoCastManager.
                    initialize(ctx, APPLICATION_ID, null, null)
                    .enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                            VideoCastManager.FEATURE_LOCKSCREEN |
                            VideoCastManager.FEATURE_WIFI_RECONNECT |
                            VideoCastManager.FEATURE_CAPTIONS_PREFERENCE |
                            VideoCastManager.FEATURE_DEBUGGING);

            mCastMgr = VideoCastManager.getInstance();

        }
        return mCastMgr;
    }
}
