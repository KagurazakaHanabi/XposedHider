package com.yaerin.xposed.hide.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.yaerin.xposed.hide.R;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
