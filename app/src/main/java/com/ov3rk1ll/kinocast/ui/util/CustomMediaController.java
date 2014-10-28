package com.ov3rk1ll.kinocast.ui.util;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

public class CustomMediaController extends MediaController {

    public CustomMediaController(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK){
            ((Activity) getContext()).onBackPressed();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
