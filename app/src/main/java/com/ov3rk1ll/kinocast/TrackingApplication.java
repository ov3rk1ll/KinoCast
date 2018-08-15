package com.ov3rk1ll.kinocast;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.Parser;


public class TrackingApplication extends Application {

    @Override
    public void onCreate() {
        //TODO Select Parser depending on settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Parser.selectParser(this, preferences.getInt("parser", KinoxParser.PARSER_ID));
        Log.i("selectParser", "ID is " + Parser.getInstance().getParserId());



        super.onCreate();
    }
}
