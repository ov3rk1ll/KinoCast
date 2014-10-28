package com.ov3rk1ll.kinocast.ui;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.TextView;

import com.google.sample.castcompanionlibrary.cast.player.VideoCastControllerActivity;


public class ColorfulVideoCastControllerActivity extends VideoCastControllerActivity {
    public static final String EXTRA_AB_COLOR = "actionbar_background_color";
    public static final String EXTRA_TEXT_COLOR = "actionbar_title_color";

    private int backgroundColor = -1;
    private int textColor = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundColor = getIntent().getIntExtra(EXTRA_AB_COLOR, -1);
        textColor = getIntent().getIntExtra(EXTRA_TEXT_COLOR, -1);

        //Color other stuff?
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(backgroundColor != -1)
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(backgroundColor));

        if(textColor != -1) {
            int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            if (actionBarTitleId > 0) {
                TextView title = (TextView) findViewById(actionBarTitleId);
                if (title != null) {
                    title.setTextColor(textColor);
                }
            }
        }
    }
}
