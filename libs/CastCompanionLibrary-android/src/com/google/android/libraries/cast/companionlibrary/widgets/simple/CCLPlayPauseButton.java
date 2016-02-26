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

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;


import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

/**
 * A cast-aware {@link ImageButton} for toggling the playback of the currently playing remote media.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *     &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLPlayPauseButton
 *         android:layout_height="wrap_content"
 *         android:layout_width="wrap_content"
 *         android:background="?attr/selectableItemBackgroundBorderless"/&gt;
 * </pre>
 *
 * By default, the following drawables are used for this button:
 * <ul>
 *     <li>for play: {@code R.drawable.ic_av_play_light}
 *     <li>for pause: {@code R.drawable.ic_av_pause_light}
 *     <li>for stop: {@code R.drawable.ic_av_stop_light}
 * </ul>
 * Clients, however, can set their own drawables by using the following custom xml attributes,
 * respectively:
 * <ul>
 *     <li>for play: {@code ccl_widget_play_drawable}
 *     <li>for pause: {@code ccl_widget_pause_drawable}
 *     <li>for stop: {@code ccl_widget_stop_drawable}
 * </ul>
 * Note that this widget will use the "stop" drawable instead of the "pause" drawable when the
 * current remote media is a live stream. The behavior, when there is no connection to a cast
 * device, can be configured by setting a {@link WidgetDisconnectPolicy}  on the custom xml
 * attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLPlayPauseButton extends ImageButton {

    private static final String TAG = "CCLPlayPauseButton";

    private VideoCastConsumerImpl mCastConsumer;
    private VideoCastManager mCastManager;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private Drawable mStopDrawable;
    private int mPlaybackState;
    private WidgetDisconnectPolicy mDisconnectPolicy;
    private static final int DEFAULT_PLAY_RESOURCE_ID = R.drawable.ic_av_play_light;
    private static final int DEFAULT_PAUSE_RESOURCE_ID = R.drawable.ic_av_pause_light;
    private static final int DEFAULT_STOP_RESOURCE_ID = R.drawable.ic_av_stop_light;
    private OnClickListener mOnClickListener;

    public CCLPlayPauseButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CCLPlayPauseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public CCLPlayPauseButton(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext()
                .obtainStyledAttributes(attrs, R.styleable.CCLPlayPauseButton, defStyleAttr,
                        defStyleRes);
        int playResourceId = typedArray
                .getResourceId(R.styleable.CCLPlayPauseButton_ccl_widget_play_drawable,
                        DEFAULT_PLAY_RESOURCE_ID);
        int pauseResourceId = typedArray
                .getResourceId(R.styleable.CCLPlayPauseButton_ccl_widget_pause_drawable,
                        DEFAULT_PAUSE_RESOURCE_ID);
        int stopResourceId = typedArray
                .getResourceId(R.styleable.CCLPlayPauseButton_ccl_widget_stop_drawable,
                        DEFAULT_STOP_RESOURCE_ID);
        int disconnectPolicyId = typedArray
                .getInt(R.styleable.CCLPlayPauseButton_ccl_widget_disconnect_policy,
                        WidgetDisconnectPolicy.VISIBLE.getValue());
        typedArray.recycle();

        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
        mPlayDrawable = getResources().getDrawable(playResourceId);
        mPauseDrawable = getResources().getDrawable(pauseResourceId);
        mStopDrawable = getResources().getDrawable(stopResourceId);
        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onRemoteMediaPlayerStatusUpdated() {
                updatePlayerStatus();
            }

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
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mCastManager.togglePlayback();
                } catch (CastException | TransientNetworkDisconnectionException |
                        NoConnectionException e) {
                    LOGE(TAG, "Failed to toggle play/pause button", e);
                }
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v);
                }
            }
        });
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

    public void setPlayPauseState(int state, int reason) {
        switch (state) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                boolean isLive = false;
                try {
                    if (mCastManager.isRemoteStreamLive()) {
                        isLive = true;
                    }
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    LogUtils.LOGE(TAG, "Failed to see if the stream is live or not", e);
                }
                setImageDrawable(isLive ? mStopDrawable : mPauseDrawable);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                setImageDrawable(mPlayDrawable);
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                setImageDrawable(mPlayDrawable);
                break;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePlayerStatus();
        applyDisconnectPolicy(mCastManager.isConnected());
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCastManager != null) {
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onDetachedFromWindow();
    }


    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    private void updatePlayerStatus() {
        int mediaStatus = mCastManager.getPlaybackStatus();
        switch (mediaStatus) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                if (mPlaybackState != MediaStatus.PLAYER_STATE_PLAYING) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PLAYING;
                    setPlayPauseState(mPlaybackState, 0);
                }
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                if (mPlaybackState != MediaStatus.PLAYER_STATE_PAUSED) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PAUSED;
                    setPlayPauseState(mPlaybackState, 0);
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                if (mPlaybackState != MediaStatus.PLAYER_STATE_BUFFERING) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                    setPlayPauseState(mPlaybackState, 0);
                }
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                switch (mCastManager.getIdleReason()) {
                    case MediaStatus.IDLE_REASON_CANCELED:
                        try {
                            if (mCastManager.isRemoteStreamLive()) {
                                if (mPlaybackState != MediaStatus.PLAYER_STATE_IDLE) {
                                    mPlaybackState = MediaStatus.PLAYER_STATE_IDLE;
                                    setPlayPauseState(mPlaybackState,
                                            MediaStatus.IDLE_REASON_CANCELED);
                                }
                            }
                        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                            LOGD(TAG, "Failed to determine if stream is live", e);
                        }
                        break;
                    case MediaStatus.IDLE_REASON_INTERRUPTED:
                        mPlaybackState = MediaStatus.PLAYER_STATE_IDLE;
                        setPlayPauseState(mPlaybackState, MediaStatus.IDLE_REASON_INTERRUPTED);
                        break;
                    default:
                        setPlayPauseState(MediaStatus.PLAYER_STATE_IDLE,
                                MediaStatus.IDLE_REASON_NONE);
                        break;
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                applyDisconnectPolicy(mCastManager.isConnected());
                setPlayPauseState(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_NONE);
                break;
            default:
                break;
        }
    }
}
