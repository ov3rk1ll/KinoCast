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

import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.MediaQueue;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

/**
 * An {@link ImageButton} that can act as a number of media controllers:
 * <ul>
 *     <li><b>Skip Next:</b> in a queue, skips to the next queue item, if there is one
 *     <li><b>Skip Previous:</b> in a queue, skips to the previous item if there is one
 *     <li><b>Fast Forward:</b> seeks forward by a specified amount
 *     <li><b>Rewind:</b> seeks backward by a specified amount
 * </ul>
 * The specific type of this widget is determined by the xml attribute {@code
 * ccl_widget_media_control_type} that can take one of the values {@code skip_next, skip_previous,
 * forward} or {@code rewind} respectively. For types {@code forward} and {@code rewind}, the
 * duration can be set by the xml attribute {@code ccl_widget_forward_duration} and {@code
 * ccl_widget_rewind_duration}, respectively (values should be set in seconds); if they are not
 * explicitly set, the default value of 30 seconds will be used.
 *
 * <p>For {@code skip_next} and {@code skip_previous}, an additional xml attribute {@code
 * ccl_widget_next_prev_visibility_policy} can be used to set the policy on whether these buttons
 * should be visible, hidden or disabled if the current position is at the end or beginning of the
 * queue, respectively. If this attribute is not set, the default policy of disabling these buttons
 * will be used.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *     &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLMediaControlButton
 *          app:ccl_widget_media_control_type="forward"
 *          app:ccl_widget_next_prev_visibility_policy="disabled"
 *          app:ccl_widget_forward_duration="30"
 *          android:layout_height="wrap_content"
 *          android:layout_width="wrap_content"
 *          android:background="?attr/selectableItemBackgroundBorderless"
 *          android:src="@drawable/ic_forward_30_white_48dp"/&gt;
 * </pre>
 *
 * The behavior when there is no connection to a cast device can be configured by setting a
 * {@link WidgetDisconnectPolicy}  on the custom xml attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLMediaControlButton extends ImageButton {

    private static final String TAG = "CCLMediaControlButton";
    private int mForwardDurationMillis;
    private int mRewindDurationMillis;
    private MediaControlType mType;
    private NextPrevVisibilityPolicy mForwardPrevPolicy;
    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mCastConsumer;
    private boolean mHasNext;
    private boolean mHasPrev;
    private OnClickListener mOnClickListener;
    private static final int DEFAULT_FORWARD_DURATION_SECONDS = 30;
    private WidgetDisconnectPolicy mDisconnectPolicy;

    enum MediaControlType {
        UNKNOWN(-1), SKIP_NEXT(0), SKIP_PREVIOUS(1), FORWARD(2), REWIND(3);

        private int mValue;

        MediaControlType(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static MediaControlType fromValue(int value) {
            switch (value) {
                case 0:
                    return SKIP_NEXT;
                case 1:
                    return SKIP_PREVIOUS;
                case 2:
                    return FORWARD;
                case 3:
                    return REWIND;
                default:
                    return UNKNOWN;
            }
        }
    }

    enum NextPrevVisibilityPolicy {
        UNKNOWN(-1), HIDDEN(0), DISABLED(1), VISIBLE(2);

        private int mValue;

        NextPrevVisibilityPolicy(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static NextPrevVisibilityPolicy fromValue(int value) {
            switch (value) {
                case 0:
                    return HIDDEN;
                case 1:
                    return DISABLED;
                case 2:
                    return VISIBLE;
                default:
                    return UNKNOWN;
            }
        }
    }

    public CCLMediaControlButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CCLMediaControlButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public CCLMediaControlButton(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CCLMediaControlButton, defStyleAttr, defStyleRes);

        int typeInteger = typedArray
                .getInt(R.styleable.CCLMediaControlButton_ccl_widget_media_control_type,
                        MediaControlType.UNKNOWN.getValue());
        int nextPrevPolicyInteger = typedArray
                .getInt(R.styleable.CCLMediaControlButton_ccl_widget_next_prev_visibility_policy,
                        NextPrevVisibilityPolicy.DISABLED.getValue());
        mForwardDurationMillis = typedArray
                .getInt(R.styleable.CCLMediaControlButton_ccl_widget_forward_duration,
                        DEFAULT_FORWARD_DURATION_SECONDS) * 1000;
        mRewindDurationMillis = typedArray
                .getInt(R.styleable.CCLMediaControlButton_ccl_widget_rewind_duration,
                        DEFAULT_FORWARD_DURATION_SECONDS) * 1000;
        int disconnectPolicyId = typedArray
                .getInt(R.styleable.CCLClosedCaptionButton_ccl_widget_disconnect_policy,
                        WidgetDisconnectPolicy.VISIBLE.getValue());
        typedArray.recycle();
        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
        mType = MediaControlType.fromValue(typeInteger);
        mForwardPrevPolicy = NextPrevVisibilityPolicy.fromValue(nextPrevPolicyInteger);

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
            public void onApplicationConnectionFailed(int errorCode) {
                applyDisconnectPolicy(false);
            }

            @Override
            public void onMediaQueueUpdated(List<MediaQueueItem> queueItems, MediaQueueItem item,
                    int repeatMode, boolean shuffle) {
                int size = 0;
                int position = 0;
                if (queueItems != null) {
                    size = queueItems.size();
                    position = queueItems.indexOf(item);
                }
                mHasNext = position < size - 1;
                mHasPrev = position > 0;
                applyDisconnectPolicy(mCastManager.isConnected());
            }
        };
        mCastManager.addVideoCastConsumer(mCastConsumer);

        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mType) {
                    case FORWARD:
                        moveForward();
                        break;
                    case REWIND:
                        rewind();
                        break;
                    case SKIP_NEXT:
                        skipNext();
                        break;
                    case SKIP_PREVIOUS:
                        skipPrevious();
                        break;
                    default:
                        LOGE(TAG, "Invalid type for CCLMediaControlButton");
                }
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v);
                }
            }
        });
        updateQueueNextPrevAvailability();
        applyDisconnectPolicy(mCastManager.isConnected());
    }

    private void updateQueueNextPrevAvailability() {
        MediaQueue mediaQueue = mCastManager.getMediaQueue();
        if (mediaQueue == null) {
            mHasNext = false;
            mHasPrev = false;
        } else {
            int size = mediaQueue.getCount();
            int position = mediaQueue.getCurrentItemPosition();
            mHasNext = position < size - 1;
            mHasPrev = position > 0;
        }
    }

    private void applyDisconnectPolicy(boolean connected) {
        if (!connected) {
            switch (mDisconnectPolicy) {
                case INVISIBLE:
                    setVisibility(INVISIBLE);
                    break;
                case GONE:
                    setVisibility(GONE);
                    break;
                case VISIBLE:
                    if (mType == MediaControlType.SKIP_NEXT
                            || mType == MediaControlType.SKIP_PREVIOUS) {

                        switch (mForwardPrevPolicy) {
                            case HIDDEN:
                                setVisibility(INVISIBLE);
                                break;
                            case DISABLED:
                                setVisibility(VISIBLE);
                                setEnabled(false);
                                break;
                            case VISIBLE:
                                setVisibility(VISIBLE);
                                setEnabled(true);
                        }
                    } else {
                        setVisibility(VISIBLE);
                    }
                    break;
            }
        } else {
            if ((mType == MediaControlType.SKIP_NEXT && !mHasNext)
                    || (mType == MediaControlType.SKIP_PREVIOUS && !mHasPrev)) {
                switch (mForwardPrevPolicy) {
                    case HIDDEN:
                        setVisibility(INVISIBLE);
                        break;
                    case DISABLED:
                        setVisibility(VISIBLE);
                        setEnabled(false);
                        break;
                    case VISIBLE:
                        setVisibility(VISIBLE);
                        setEnabled(true);
                }
            } else if ((mType == MediaControlType.SKIP_NEXT)
                    || (mType == MediaControlType.SKIP_PREVIOUS)) {
                setVisibility(VISIBLE);
                setEnabled(true);
            }
        }
    }

    private void skipNext() {
        if (mHasNext) {
            try {
                mCastManager.queueNext(null);
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                LOGE(TAG, "onReceive() Failed to skip to the previous in queue");
            }
        }
    }

    private void skipPrevious() {
        if (mHasPrev) {
            try {
                mCastManager.queuePrev(null);
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                LOGE(TAG, "onReceive() Failed to skip to the previous in queue");
            }
        }
    }

    private void rewind() {
        try {
            mCastManager.forward(-mRewindDurationMillis);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LOGE(TAG, "onReceive() Failed to forward the media");
        }
    }

    private void moveForward() {
        try {
            mCastManager.forward(mForwardDurationMillis);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LOGE(TAG, "onReceive() Failed to forward the media");
        }
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
        if (l != null) {
            mOnClickListener = l;
        }
    }
}
