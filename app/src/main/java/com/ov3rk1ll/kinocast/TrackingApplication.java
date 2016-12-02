package com.ov3rk1ll.kinocast;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.ov3rk1ll.kinocast.api.Parser;

import io.fabric.sdk.android.Fabric;

public class TrackingApplication extends Application {

    @Override
    public void onCreate() {
        //TODO Select Parser depending on settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //Parser.selectParser(preferences.getInt("parser", KinoxParser.PARSER_ID));
        Parser.selectByName(getApplicationContext(), "kinox");
        // Log.i("selectParser", "ID is " + Parser.getInstance().getParserId());
        super.onCreate();
        Fabric.with(this, new Crashlytics());
    }
}
