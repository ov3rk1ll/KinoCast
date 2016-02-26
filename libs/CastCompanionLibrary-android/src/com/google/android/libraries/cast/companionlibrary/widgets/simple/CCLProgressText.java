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
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;
import com.google.android.libraries.cast.companionlibrary.widgets.ProgressWatcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A cast-aware {@link TextView} that can either show the current elapsed time of the remote media
 * or the total duration of remote media where, the custom xml attribute {@code ccl_widget_type}
 * determines the behavior. This attribute can take one of the two values:
 * <ul>
 *     <li>{@code progress}
 *     <li>{@code duration}
 * </ul>
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *      &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLProgressText
 *          app:ccl_widget_type="progress"
 *          android:layout_width="wrap_content"
 *          android:layout_height="wrap_content" />
 * </pre>
 *
 * The behavior when there is no connection to a cast device can be configured by setting a
 * {@link WidgetDisconnectPolicy}  on the custom xml attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLProgressText extends TextView {

    private static final String TAG = "CCLProgressText";
    private VideoCastManager mCastManager;
    private ProgressWatcher mProgressWatcher;
    private ProgressViewerType mType;
    private Handler mHandler;
    private WidgetDisconnectPolicy mDisconnectPolicy;
    private VideoCastConsumerImpl mCastConsumer;

    public enum ProgressViewerType {
        PROGRESS_VIEWER(0), DURATION_VIEWER(1);

        private int mValue;

        ProgressViewerType(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static ProgressViewerType fromValue(int value) {
            switch (value) {
                case 1:
                    return DURATION_VIEWER;
                case 0:
                default:
                    return PROGRESS_VIEWER;
            }
        }
    }

    @TargetApi(21)
    public CCLProgressText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public CCLProgressText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CCLProgressText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mHandler = new Handler();
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CCLProgressText, defStyleAttr, defStyleRes);

        mType = ProgressViewerType.PROGRESS_VIEWER;
        int typeInteger = a.getInt(R.styleable.CCLProgressText_ccl_widget_type,
                ProgressViewerType.PROGRESS_VIEWER.getValue());
        int disconnectPolicyId = a.getInt(R.styleable.CCLProgressText_ccl_widget_disconnect_policy,
                WidgetDisconnectPolicy.VISIBLE.getValue());
        a.recycle();

        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
        mType = ProgressViewerType.fromValue(typeInteger);

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
        mProgressWatcher = new

                ProgressWatcher() {

                    @Override
                    public void setProgress(final int currentPosition, final int duration) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                update(currentPosition, duration);
                            }
                        });
                    }
                };

        mCastManager.addProgressWatcher(mProgressWatcher);
        if (mCastManager.isConnected()) {
            try {
                update((int) mCastManager.getCurrentMediaPosition(),
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

    private void update(int currentPosition, int duration) {
        switch (mType) {
            case PROGRESS_VIEWER:
                setValue(currentPosition);
                break;
            case DURATION_VIEWER:
                setValue(duration);
                break;
            default:
                LogUtils.LOGE(TAG,
                        "Unknown CCLProgressText type: " + mType);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCastManager != null) {
            mCastManager.removeProgressWatcher(mProgressWatcher);
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onDetachedFromWindow();
    }

    ProgressViewerType getType() {
        return mType;
    }

    void setValue(int progress) {
        setText(Utils.formatMillis(progress));
    }
}
