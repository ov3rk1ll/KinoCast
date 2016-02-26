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
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.tracks.ui.TracksChooserDialog;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

/**
 * An {@link ImageButton} that opens a dialog to configure text and audio tracks for the currently
 * playing media item.
 *
 * <p>Here is an example of how this widget can be added to a layout:
 * <pre>
 *      &lt;com.google.android.libraries.cast.companionlibrary.widgets.simple.CCLClosedCaptionButton
 *          android:layout_height="wrap_content"
 *          android:background="?attr/selectableItemBackgroundBorderless"
 *          android:layout_width="wrap_content"
 *          android:src="@drawable/cc" /&gt;
 * </pre>
 *
 * Clients can set the image for this button. Clients can set the image to
 * {@code R.drawable.cc} that is provided by the library or can use their own. The behavior when
 * there is no connection to a cast device can be configured by setting a
 * {@link WidgetDisconnectPolicy}  on the custom xml attribute {@code ccl_widget_disconnect_policy}.
 */
public class CCLClosedCaptionButton extends ImageButton {

    private static final String DIALOG_TAG = "dialog-tag";
    private static final String TAG = "CCLClosedCaptionButton";
    private VideoCastManager mCastManager;
    private WidgetDisconnectPolicy mDisconnectPolicy;
    private VideoCastConsumerImpl mCastConsumer;
    private OnClickListener mOnClickListener;

    private static final int CC_ENABLED = 1;
    private static final int CC_DISABLED = 2;
    private static final int CC_HIDDEN = 3;

    private boolean mFeatureAvailable;

    private int mCcState;

    public CCLClosedCaptionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CCLClosedCaptionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public CCLClosedCaptionButton(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CCLClosedCaptionButton, defStyleAttr, defStyleRes);
        int disconnectPolicyId = typedArray
                .getInt(R.styleable.CCLClosedCaptionButton_ccl_widget_disconnect_policy,
                        WidgetDisconnectPolicy.VISIBLE.getValue());
        typedArray.recycle();
        mDisconnectPolicy = WidgetDisconnectPolicy.fromValue(disconnectPolicyId);
        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onRemoteMediaPlayerStatusUpdated() {
                updateClosedCaptionState();
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
                    showClosedCaptionChooserDialog(getContext());
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    LOGE(TAG, "Failed to show closed caption dialog", e);
                }
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v);
                }
            }
        });
        updateClosedCaptionState();
        applyDisconnectPolicy(mCastManager.isConnected());
    }

    private void updateClosedCaptionState() {
        try {
            mFeatureAvailable =
                    mCastManager.isFeatureEnabled(CastConfiguration.FEATURE_CAPTIONS_PREFERENCE)
                            && mCastManager.getTracksPreferenceManager().isCaptionEnabled();
            mCcState = CC_HIDDEN;
            if (!mCastManager.isConnected()) {
                return;
            }
            MediaInfo currentMedia = mCastManager.getRemoteMediaInformation();
            if (mFeatureAvailable && currentMedia != null) {
                List<MediaTrack> tracks = currentMedia.getMediaTracks();
                boolean hasAudioTextTracks = Utils.hasAudioOrTextTrack(tracks);
                mCcState = hasAudioTextTracks ? CC_ENABLED : CC_DISABLED;
                setEnabled(hasAudioTextTracks);
            }

        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LogUtils.LOGE(TAG, "updateClosedCaptionState(): Failed to get the media information");
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCastManager != null) {
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onDetachedFromWindow();
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
                    if (!mFeatureAvailable || mCcState == CC_HIDDEN) {
                        setEnabled(false);
                    }
                    break;
            }
        }
    }

    private void showClosedCaptionChooserDialog(Context context)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            FragmentTransaction transaction = activity.getSupportFragmentManager()
                    .beginTransaction();
            Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
            if (prev != null) {
                transaction.remove(prev);
            }
            transaction.addToBackStack(null);

            // Create and show the dialog.
            TracksChooserDialog dialogFragment = TracksChooserDialog
                    .newInstance(mCastManager.getRemoteMediaInformation());
            dialogFragment.show(transaction, DIALOG_TAG);
        }
    }
}
