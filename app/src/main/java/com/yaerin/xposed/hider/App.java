package com.yaerin.xposed.hider;

import android.app.Application;

import com.yaerin.xposed.hider.util.Crashlytics;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Crashlytics(this));
    }
}
