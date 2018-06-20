package com.yaerin.xposed.hide.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.yaerin.xposed.hide.R;
import com.yaerin.xposed.hide.adapter.AppsAdapter;
import com.yaerin.xposed.hide.bean.AppInfo;
import com.yaerin.xposed.hide.util.Utilities;
import com.yaerin.xposed.hide.widget.AppView;

import java.util.ArrayList;
import java.util.List;

import static com.yaerin.xposed.hide.util.Utilities.getAppList;
import static com.yaerin.xposed.hide.util.Utilities.updateAppList;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private List<AppInfo> mApps;

    public static boolean isEnabled() {
        Log.i(TAG, "#include <iostream>                           ");
        Log.i(TAG, "                                              ");
        Log.i(TAG, "int main(void) {                              ");
        Log.i(TAG, "    std::cout << \"Hello World\" << std::endl;");
        Log.i(TAG, "    return 0;                                 ");
        Log.i(TAG, "}                                             ");
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApps = new ArrayList<>();

        ListView appsView = findViewById(R.id.apps);
        if (isEnabled()) {
            findViewById(R.id.tip_reboot).setVisibility(View.GONE);
        }
        appsView.setOnItemClickListener((parent, view, position, id) -> {
            AppView v = (AppView) view;
            AppInfo app = v.getAppInfo();
            if (v.isChecked()) {
                app.setDisabled(false);
                mApps.remove(app);
                v.setChecked(false);
            } else {
                app.setDisabled(true);
                mApps.add(app);
                v.setChecked(true);
            }
        });
        new Thread(() -> {
            List<AppInfo> apps = getAppList(this);
            if (apps.size() == 0) {
                updateAppList(this);
                apps = getAppList(this);
            }
            for (AppInfo app : apps) {
                if (app.isDisabled()) {
                    mApps.add(app);
                }
            }
            List<AppInfo> finalApps = apps;
            runOnUiThread(() -> {
                appsView.setAdapter(new AppsAdapter(this, finalApps));
                findViewById(R.id.progress).setVisibility(View.GONE);
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about: {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        Utilities.putConfig(this, mApps);
        super.onPause();
    }
}
