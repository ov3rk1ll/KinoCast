/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.widgets.simple;


import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions
        .TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.widgets.ProgressWatcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * A cast-aware linear {@link ProgressBar} that follows the progress of the currently playing remote
 * media. The behavior when there is no connection to a cast device can be configured by setting a
 * {@link WidgetDisconnectPolicy}  on the custom xml attribute {@code ccl_widget_disconnect_policy}.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *      &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLProgressBar
 *          style="?android:attr/progressBarStyleHorizontal"
 *          android:layout_width="300dp"
 *          android:layout_height="20dp"/&gt;
 * </pre>
 */
public class CCLProgressBar extends ProgressBar implements ProgressWatcher {

    private static final String TAG = "CCLProgressBar";
    private VideoCastManager mCastManager;
    private WidgetDisconnectPolicy mDisconnectPolicy;
    private VideoCastConsumerImpl mCastConsumer;

    public CCLProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CCLProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public CCLProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CCLProgressBar, defStyleAttr, defStyleRes);
        int disconnectPolicyId = typedArray
                .getInt(R.styleable.CCLProgressBar_ccl_widget_disconnect_policy,
                        WidgetDisconnectPolicy.VISIBLE.getValue());
        typedArray.recycle();

        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onDisconnected() {
                applyDisconnectPolicy(false);
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                applyDisconnectPolicy(false);
            }

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                    boolean wasLaunched) {
                applyDisconnectPolicy(true);
            }
        };
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.addProgressWatcher(this);
        if (mCastManager.isConnected()) {
            try {
                setProgress((int) mCastManager.getCurrentMediaPosition(),
                        (int) mCastManager.getMediaDuration());

            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                LogUtils.LOGE(TAG, "init(): Failed to get the current media position or duration",
                        e);
            }
        }
    }

    private void applyDisconnectPolicy(boolean connected) {
        if (connected) {
            setVisibility(VISIBLE);
        } else {
            switch (mDisconnectPolicy) {
                case INVISIBLE:
                    setVisibility(INVISIBLE);
                    break;
                case GONE:
                    setVisibility(GONE);
                    break;
                case VISIBLE:
                    setVisibility(VISIBLE);
                    break;
            }
        }
    }

    @Override
    public void setProgress(int currentPosition, int duration) {
        setProgress(currentPosition);
        setMax(duration);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCastManager != null) {
            mCastManager.removeProgressWatcher(this);
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onDetachedFromWindow();
    }
}
