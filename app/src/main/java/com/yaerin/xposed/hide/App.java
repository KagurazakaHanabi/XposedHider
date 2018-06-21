package com.yaerin.xposed.hide;

import android.app.Application;

import com.yaerin.xposed.hide.util.Crashlytics;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Crashlytics(this));
    }
}
