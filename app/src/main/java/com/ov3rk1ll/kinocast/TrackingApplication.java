package com.ov3rk1ll.kinocast;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.Parser;

import java.util.HashMap;

public class TrackingApplication extends Application {

    @Override
    public void onCreate() {
        //TODO Select Parser depending on settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Parser.selectParser(preferences.getInt("parser", KinoxParser.PARSER_ID));
        Log.i("selectParser", "ID is " + Parser.getInstance().getParserId());
        super.onCreate();
    }

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(getString(R.string.ga_trackingId));
            t.enableAutoActivityTracking(true);
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
}
