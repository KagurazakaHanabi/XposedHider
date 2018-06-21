package com.yaerin.xposed.hider.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.yaerin.xposed.hider.R;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
