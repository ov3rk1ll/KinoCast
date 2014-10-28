package com.ov3rk1ll.kinocast.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.util.CustomMediaController;
import com.ov3rk1ll.kinocast.ui.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PlayerActivity extends Activity {
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final boolean TOGGLE_ON_CLICK = true;
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private SystemUiHider mSystemUiHider;
    private Uri mVideoUri;
    private CustomMediaController mMediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        mVideoUri = getIntent().getData();

        final VideoView videoView = (VideoView) findViewById(R.id.fullscreen_content);
        final View controlsView = findViewById(R.id.fullscreen_content_controls);

        mMediaController = new CustomMediaController(this);

        mSystemUiHider = SystemUiHider.getInstance(this, getWindow().getDecorView(), HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    @Override
                    public void onVisibilityChange(boolean visible) {
                        //controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        if (visible) {
                            mMediaController.show();
                        } else {
                            mMediaController.hide();
                        }

                        if (visible && AUTO_HIDE) {
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        controlsView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (TOGGLE_ON_CLICK) {
                        mSystemUiHider.toggle();
                    } else {
                        mSystemUiHider.show();
                    }
                }
                return true;
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final VideoView videoView = (VideoView) findViewById(R.id.fullscreen_content);

        videoView.setVideoURI(mVideoUri);

        final FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        final View videoProgressView = View.inflate(getApplicationContext(), R.layout.video_loading_progress, null);

        decor.addView(videoProgressView);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                decor.removeView(videoProgressView);
                videoView.setMediaController(mMediaController);
                mMediaController.setAnchorView(controlsView);
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                (new AlertDialog.Builder(PlayerActivity.this))
                        .setTitle(getString(R.string.player_error_dialog_title))
                        .setMessage(getString(R.string.player_unsupported_format) + "\n\n(#" + what + "E" + extra + ")")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
                return true;
            }
        });

        videoView.start();
        delayedHide(100);
    }

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
