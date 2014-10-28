package com.ov3rk1ll.kinocast.utils;

import android.app.Activity;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;

public class ShowcaseHelper {
    public static void showSeriesHelp(Activity activity, OnShowcaseEventListener onShowcaseEventListener){
        if(!BuildConfig.ENABLE_HELP) return;
        new ShowcaseView.Builder(activity, true)
                .setTarget(new ViewTarget(R.id.spinnerSeason, activity))
                .setContentTitle(activity.getString(R.string.showcast_series_title))
                .setContentText(activity.getString(R.string.showcast_series_text))
                .setStyle(R.style.ShowcaseViewDark)
                .setShowcaseEventListener(onShowcaseEventListener)
                .singleShot(R.id.spinnerSeason)
                .hideOnTouchOutside()
                .build();
    }

    public static void showMirrorHelp(Activity activity){
        if(!BuildConfig.ENABLE_HELP) return;
        new ShowcaseView.Builder(activity, true)
                .setTarget(new ViewTarget(R.id.spinnerMirror, activity))
                .setContentTitle(activity.getString(R.string.showcast_mirror_title))
                .setContentText(activity.getString(R.string.showcast_mirror_text))
                .setStyle(R.style.ShowcaseViewDark)
                .singleShot(R.id.spinnerMirror)
                .hideOnTouchOutside()
                .build();
    }

    public static void showChromecastHelp(Activity activity){
        if(!BuildConfig.ENABLE_HELP) return;
        new ShowcaseView.Builder(activity, true)
                .setTarget(new ActionViewTarget(activity, ActionViewTarget.Type.MEDIA_ROUTE_BUTTON))
                .setContentTitle(activity.getString(R.string.showcast_chromecast_title))
                .setContentText(activity.getString(R.string.showcast_chromecast_text))
                .setStyle(R.style.ShowcaseViewDark)
                .singleShot(R.id.media_route_menu_item)
                .hideOnTouchOutside()
                .build();
    }

    private static boolean showingBookmarkHelp = false;
    public static void showBookmarkHelp(Activity activity, OnShowcaseEventListener onShowcaseEventListener){
        if(!BuildConfig.ENABLE_HELP) return;
        if(showingBookmarkHelp) return;
        showingBookmarkHelp = true;
        Target t = new ActionItemTarget(activity, R.id.action_bookmark_off);
        try{
            t.getPoint();
        } catch (NullPointerException e){
            e.printStackTrace();
            t = new ActionViewTarget(activity, ActionViewTarget.Type.OVERFLOW);
        }
        new ShowcaseView.Builder(activity, true)
                .setTarget(t)
                .setContentTitle(activity.getString(R.string.showcast_bookmarks_title))
                .setContentText(activity.getString(R.string.showcast_bookmarks_text))
                .setStyle(R.style.ShowcaseViewDark)
                .setShowcaseEventListener(onShowcaseEventListener)
                .singleShot(R.id.action_bookmark_off)
                .hideOnTouchOutside()
                .build();
    }
}
