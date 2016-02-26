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
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions
        .TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.FetchBitmapTask;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.List;

/**
 * An {@link ImageView} that shows the album art for the currently playing remote media.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *      &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLMediaImageView
 *          android:scaleType="fitCenter"
 *          android:layout_width="100dp"
 *          app:ccl_widget_image_index="0"
 *          android:layout_height="wrap_content" />
 * </pre>
 * This widget obtains the url of the image from: {@link MediaMetadata#getImages()} at the index
 * that can be provided by the custom xml attribute {@code ccl_widget_image_index} (default index is
 * 0). In addition, if it fails to load the image, a placeholder image can be shown; this
 * placeholder image can be set by the custom xml attribute {@code ccl_widget_placeholder_drawable}.
 * The behavior when there is no connection to a cast device can be configured by setting a
 * {@link WidgetDisconnectPolicy}  on the custom xml attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLMediaImageView extends ImageView {

    private static final String TAG = "CCLMediaImageButton";
    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mCastConsumer;
    private FetchBitmapTask mFetchBitmapTask;
    private Uri mCachedUri;
    private int mImageIndex;
    private int mPlaceHolderResourceId;
    private static final int DEFAULT_PLACEHOLDER_IMAGE_RESOURCE_ID
            = R.drawable.album_art_placeholder_large;
    private static final int DEFAULT_IMAGE_INDEX = 0;
    private WidgetDisconnectPolicy mDisconnectPolicy;

    public CCLMediaImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CCLMediaImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public CCLMediaImageView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext()
                .obtainStyledAttributes(attrs, R.styleable.CCLMediaImageView, defStyleAttr,
                        defStyleRes);
        mPlaceHolderResourceId = typedArray
                .getResourceId(R.styleable.CCLMediaImageView_ccl_widget_placeholder_drawable,
                        DEFAULT_PLACEHOLDER_IMAGE_RESOURCE_ID);
        mImageIndex = typedArray.getInt(R.styleable.CCLMediaImageView_ccl_widget_image_index,
                DEFAULT_IMAGE_INDEX);
        int disconnectPolicyId = typedArray
                .getInt(R.styleable.CCLMediaImageView_ccl_widget_disconnect_policy,
                        WidgetDisconnectPolicy.VISIBLE.getValue());
        typedArray.recycle();

        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onRemoteMediaPlayerMetadataUpdated() {
                updateImage();
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
        updateImage();
        applyDisconnectPolicy(mCastManager.isConnected());
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
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        if (mFetchBitmapTask != null) {
            mFetchBitmapTask.cancel(true);
        }

        super.onDetachedFromWindow();
    }

    private void updateImage() {
        try {
            MediaInfo info = mCastManager.getRemoteMediaInformation();
            if (info == null) {
                showPlaceHolder();
                return;
            }
            List<WebImage> images = info.getMetadata().getImages();
            final Uri imgUri;
            if (!images.isEmpty()) {
                // grab the image given by the index, if populated, otherwise lower the index to
                // the highest available index in the list of images
                if (images.size() > mImageIndex) {
                    imgUri = images.get(mImageIndex).getUrl();
                } else {
                    imgUri = images.get(images.size() - 1).getUrl();
                }

            } else {
                // we don't have a url for image so get a placeholder image from resources
                showPlaceHolder();
                return;
            }
            if (mCachedUri != null && imgUri.toString().equals(mCachedUri.toString())) {
                // we are already showing the cached image, no need to fetch it again
                LogUtils.LOGD(TAG, "No need to fetch a new image");
                return;
            }
            LogUtils.LOGD(TAG, "Fetching a new image");
            mFetchBitmapTask = new FetchBitmapTask() {
                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if (bitmap == null) {
                        bitmap = BitmapFactory.decodeResource(getResources(),
                                R.drawable.album_art_placeholder);
                    }
                    mCachedUri = imgUri;
                    setImageBitmap(bitmap);
                    if (this == mFetchBitmapTask) {
                        mFetchBitmapTask = null;
                    }
                }
            };

            mFetchBitmapTask.execute(imgUri);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LogUtils.LOGE(TAG, "Failed to get remote media information");
            showPlaceHolder();
        }
    }

    private void showPlaceHolder() {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), mPlaceHolderResourceId);
        setImageBitmap(bm);
    }
}
