package com.yaerin.xposed.hider.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.SearchView;

import com.yaerin.xposed.hider.R;
import com.yaerin.xposed.hider.adapter.AppsAdapter;
import com.yaerin.xposed.hider.bean.AppInfo;
import com.yaerin.xposed.hider.util.Utilities;
import com.yaerin.xposed.hider.widget.AppView;

import java.util.ArrayList;
import java.util.List;

import static com.yaerin.xposed.hider.util.Utilities.getAppList;
import static com.yaerin.xposed.hider.util.Utilities.updateAppList;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private AppsAdapter mAdapter;

    private List<AppInfo> mApps = new ArrayList<>();
    private List<AppInfo> mMatches = new ArrayList<>();
    private List<AppInfo> mConfig = new ArrayList<>();

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

        ListView appsView = findViewById(R.id.apps);
        if (isEnabled()) {
            findViewById(R.id.tip_reboot).setVisibility(View.GONE);
        }
        appsView.setOnItemClickListener((parent, view, position, id) -> {
            AppView v = (AppView) view;
            AppInfo app = v.getAppInfo();
            if (v.isChecked()) {
                app.setDisabled(false);
                mConfig.remove(app);
                v.setChecked(false);
            } else {
                app.setDisabled(true);
                mConfig.add(app);
                v.setChecked(true);
            }
        });
        new Thread(() -> {
            mApps = getAppList(this);
            if (mApps.size() == 0) {
                updateAppList(this);
                mApps = getAppList(this);
            }
            for (AppInfo app : mApps) {
                if (app.isDisabled()) {
                    mConfig.add(app);
                }
            }
            runOnUiThread(() -> {
                appsView.setAdapter(mAdapter = new AppsAdapter(this, mApps));
                findViewById(R.id.progress).setVisibility(View.GONE);
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        SearchView sv = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        sv.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        sv.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mMatches.clear();
                for (AppInfo app : mApps) {
                    if (app.getLabel().contains(newText) ||
                            app.getPackageName().contains(newText)) {
                        mMatches.add(app);
                    }
                }
                if (mAdapter.getAppList().equals(mApps)) {
                    mAdapter.setAppList(mMatches);
                }
                mAdapter.notifyDataSetChanged();
                return true;
            }
        });
        sv.setOnCloseListener(() -> {
            mAdapter.setAppList(mApps);
            mAdapter.notifyDataSetChanged();
            return false;
        });
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
        Utilities.putConfig(this, mConfig);
        super.onPause();
    }
}
