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

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions
        .TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.widgets.ProgressWatcher;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

/**
 * A cast-aware {@link SeekBar} that reflects and can control the current position of the remote
 * media.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *      &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLSeekBar
 *          android:layout_width="300dp"
 *          android:layout_height="20dp"/&gt;
 * </pre>
 *
 * It is common to link this widget with an instance of {@link CCLProgressText} (of type
 * {@code progress}) so that when user is seeking, the changing value is reflected in that widget.
 * To this end, clients should set the custom xml attribute {@code ccl_widget_progress_text} to
 * point to the associated {@link CCLProgressText}. The behavior when there is no connection to a
 * cast device can be configured by setting a {@link WidgetDisconnectPolicy}  on the custom xml
 * attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLSeekBar extends SeekBar implements ProgressWatcher {

    private static final String TAG = "CCLSeekBar";
    private int mProgressTextId;
    private VideoCastManager mCastManager;
    private boolean mInProgress;
    private CCLProgressText mProgressText;
    private WidgetDisconnectPolicy mDisconnectPolicy;
    private VideoCastConsumerImpl mCastConsumer;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    @TargetApi(21)
    public CCLSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public CCLSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CCLSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mCastManager = VideoCastManager.getInstance();
        mCastManager.addProgressWatcher(this);
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mProgressTextId != 0 && mProgressText == null) {
                    mProgressText = (CCLProgressText) getReferenceViewFromActivity(mProgressTextId);
                }
                if (mProgressText != null) {
                    mProgressText.setValue(progress);
                }
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mInProgress = true;
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStartTrackingTouch(seekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mInProgress = false;
                int mediaStatus = mCastManager.getPlaybackStatus();
                try {
                    if (mediaStatus == MediaStatus.PLAYER_STATE_PLAYING) {
                        mCastManager.play(seekBar.getProgress());
                    } else if (mediaStatus == MediaStatus.PLAYER_STATE_PAUSED) {
                        mCastManager.seek(seekBar.getProgress());
                    }
                } catch (Exception e) {
                    LOGE(TAG, "Failed to complete seek", e);
                }
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStopTrackingTouch(seekBar);
                }
            }
        });
        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.CCLSeekBar, defStyleAttr, defStyleRes);
        mProgressTextId = a.getResourceId(R.styleable.CCLSeekBar_ccl_widget_progress_text, 0);
        int disconnectPolicyId = a.getInt(R.styleable.CCLSeekBar_ccl_widget_disconnect_policy,
                WidgetDisconnectPolicy.VISIBLE.getValue());
        a.recycle();

        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
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

    private View getReferenceViewFromActivity(int resourceId) {
        if (getContext() instanceof Activity) {
            return ((Activity) getContext()).findViewById(resourceId);
        }
        return null;
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
    protected void onDetachedFromWindow() {
        if (mCastManager != null) {
            mCastManager.removeProgressWatcher(this);
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setProgress(int currentPosition, int duration) {
        if (!mInProgress) {
            setMax(duration);
            setProgress(currentPosition);
        }
    }

    public void setProgressText(CCLProgressText progressText) {
        if (progressText.getType() != CCLProgressText.ProgressViewerType.PROGRESS_VIEWER) {
            return;
        }
        mProgressText = progressText;
    }
}
