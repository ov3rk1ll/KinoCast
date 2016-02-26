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
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions
        .TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.Calendar;

/**
 * A cast-aware {@link TextView} for showing metadata of the currently playing remote media.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *      &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLMediaInfoText
 *          app:ccl_widget_info="title"
 *          android:layout_width="wrap_content"
 *          android:layout_height="wrap_content"/&lt;
 * </pre>
 *
 * By setting the custom xml attribute {@code ccl_widget_info}, the appropriate metadata can be
 * selected to be shown. Valid values for this attribute are:
 * <ul>
 *     <li>{@code title}
 *     <li>{@code subtitle}
 *     <li>{@code studio}
 *     <li>{@code album_artist}
 *     <li>{@code album_title}
 *     <li>{@code composer}
 *     <li>{@code series_title}
 *     <li>{@code season_number}
 *     <li>{@code episode_number}
 *     <li>{@code release_date}
 * </ul>
 * For {@code release_date} metadata, the default formatting is to use the localized
 * {@link DateUtils#formatDateTime(Context, long, int)} with the flag
 * {@code DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR}. If a different format is
 * desired, one can use the {@link #setDateFormatter(CCLDateFormatter)} to control the formatting.
 * The behavior when there is no connection to a cast device can be configured by setting a
 * {@link WidgetDisconnectPolicy}  on the custom xml attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLMediaInfoText extends TextView {

    private static final String TAG = "CCLMediaInfoText";
    private VideoCastManager mCastManager;
    private MediaInfoType mType;
    private VideoCastConsumerImpl mCastConsumer;
    private WidgetDisconnectPolicy mDisconnectPolicy;
    private CCLDateFormatter mDateFormatter;

    public enum MediaInfoType {
        UNKNOWN(-1), TITLE(0), SUBTITLE(1), STUDIO(2), ALBUM_ARTIST(3), ALBUM_TITLE(4),
        COMPOSER(5), SERIES_TITLE(6), SEASON_NUMBER(7), EPISODE_NUMBER(8), RELEASE_DATE(9);

        private int mValue;

        MediaInfoType(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static MediaInfoType fromValue(int value) {
            switch (value) {
                case 0:
                    return TITLE;
                case 1:
                    return SUBTITLE;
                case 2:
                    return STUDIO;
                case 3:
                    return ALBUM_ARTIST;
                case 4:
                    return ALBUM_TITLE;
                case 5:
                    return COMPOSER;
                case 6:
                    return SERIES_TITLE;
                case 7:
                    return SEASON_NUMBER;
                case 8:
                    return EPISODE_NUMBER;
                case 9:
                    return RELEASE_DATE;
                default:
                    return UNKNOWN;
            }
        }
    }

    public CCLMediaInfoText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CCLMediaInfoText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public CCLMediaInfoText(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Sets the {@link CCLDateFormatter} to be used in formatting "Release Date". If none is set, it
     * will use the default format defined by {@link DateUtils#formatDateTime(Context, long, int)}
     * with the flag {@code DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR}
     */
    public void setDateFormatter(CCLDateFormatter formatter) {
        mDateFormatter = formatter;
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CCLMediaInfoText, defStyleAttr, defStyleRes);

        int typeInteger = typedArray.getInt(R.styleable.CCLMediaInfoText_ccl_widget_info, -1);
        int disconnectPolicyId = typedArray.getInt(R.styleable.CCLMediaInfoText_ccl_widget_disconnect_policy,
                WidgetDisconnectPolicy.VISIBLE.getValue());
        typedArray.recycle();

        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);

        mType = MediaInfoType.fromValue(typeInteger);
        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onRemoteMediaPlayerMetadataUpdated() {
                updateText();
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
        updateText();
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

    private void updateText() {
        try {
            MediaInfo mediaInfo = mCastManager.getRemoteMediaInformation();
            if (mediaInfo != null) {
                final String text = getInfo(mType, mediaInfo);
                if (text != null) {
                    setText(text);
                }
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LogUtils.LOGE(TAG, "Failed to get media info");
            setText("");
        }
    }

    private String getInfo(MediaInfoType type, MediaInfo mediaInfo) {
        MediaMetadata metadata = mediaInfo.getMetadata();
        switch (type) {
            case TITLE:
                return metadata.getString(MediaMetadata.KEY_TITLE);
            case SUBTITLE:
                return metadata.getString(MediaMetadata.KEY_SUBTITLE);
            case STUDIO:
                return metadata.getString(MediaMetadata.KEY_STUDIO);
            case ALBUM_ARTIST:
                return metadata.getString(MediaMetadata.KEY_ALBUM_ARTIST);
            case ALBUM_TITLE:
                return metadata.getString(MediaMetadata.KEY_ALBUM_TITLE);
            case COMPOSER:
                return metadata.getString(MediaMetadata.KEY_COMPOSER);
            case SERIES_TITLE:
                return metadata.getString(MediaMetadata.KEY_SERIES_TITLE);
            case SEASON_NUMBER:
                return metadata.getString(MediaMetadata.KEY_SEASON_NUMBER);
            case EPISODE_NUMBER:
                return String.valueOf(metadata.getInt(MediaMetadata.KEY_EPISODE_NUMBER));
            case RELEASE_DATE:
                Calendar releaseCalendar = metadata.getDate(MediaMetadata.KEY_RELEASE_DATE);
                if (releaseCalendar != null) {
                    long releaseMillis = releaseCalendar.getTimeInMillis();
                    return getFormattedDate(releaseMillis);
                }
        }

        return null;
    }

    private String getFormattedDate(long millis) {
        if (mDateFormatter != null) {
            return mDateFormatter.formatDate(millis);
        }

        return DateUtils.formatDateTime(getContext(), millis,
                DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCastManager != null) {
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onDetachedFromWindow();
    }

    /**
     * An interface to define the format for showing "Release Date".
     */
    public interface CCLDateFormatter {
        String formatDate(long millis);
    }
}
