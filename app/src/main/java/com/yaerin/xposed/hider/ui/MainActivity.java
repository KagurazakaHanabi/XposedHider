package com.yaerin.xposed.hider.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.SearchView;

import com.yaerin.xposed.hider.R;
import com.yaerin.xposed.hider.adapter.AppsAdapter;
import com.yaerin.xposed.hider.bean.AppInfo;
import com.yaerin.xposed.hider.util.ConfigUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yaerin.xposed.hider.C.PREF_SHOW_SYSTEM_APP;
import static com.yaerin.xposed.hider.util.Utilities.getAppList;

public class MainActivity extends Activity {

    private ListView mAppsView;

    private AppsAdapter mAdapter;

    private SharedPreferences mPreferences;

    private List<AppInfo> mApps = new ArrayList<>();
    private List<AppInfo> mMatches = new ArrayList<>();
    private Set<String> mConfig = ConfigUtils.get() != null ? ConfigUtils.get() : new HashSet<>();
    private boolean mShowSystemApp = false;

    public static boolean isEnabled() {
        return false;
    }

    private void setCheckedItems() {
        List<AppInfo> apps = mAdapter.getAppList();
        for (int i = 0; i < apps.size(); i++) {
            mAppsView.setItemChecked(i, mConfig.contains(apps.get(i).getPackageName()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppsView = findViewById(R.id.apps);
        if (isEnabled()) {
            findViewById(R.id.tip_reboot).setVisibility(View.GONE);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mShowSystemApp = mPreferences.getBoolean(PREF_SHOW_SYSTEM_APP, false);

        mAppsView.setOnItemClickListener((parent, view, position, id) -> {
            if (mAppsView.isItemChecked(position)) {
                mConfig.add(mAdapter.getAppList().get(position).getPackageName());
            } else {
                mConfig.remove(mAdapter.getAppList().get(position).getPackageName());
            }
        });

        new Thread(() -> {
            mApps = getAppList(this, mShowSystemApp);
            runOnUiThread(() -> {
                mAppsView.setAdapter(mAdapter = new AppsAdapter(this, mApps));
                setCheckedItems();
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
                newText=newText.toLowerCase();
                for (AppInfo app : mApps) {
                    if (app.getLabel().toLowerCase().contains(newText) || app.getPackageName().contains(newText)) {
                        mMatches.add(app);
                    }
                }
                if (mAdapter.getAppList().equals(mApps)) {
                    mAdapter.setAppList(mMatches);
                }
                mAdapter.notifyDataSetChanged();
                setCheckedItems();
                return true;
            }
        });
        sv.setOnCloseListener(() -> {
            mAdapter.setAppList(mApps);
            mAdapter.notifyDataSetChanged();
            setCheckedItems();
            return false;
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean b = mPreferences.getBoolean(PREF_SHOW_SYSTEM_APP, false);
        if (mShowSystemApp != b) {
            mShowSystemApp = b;
            mApps.clear();
            mApps.addAll(getAppList(this, mShowSystemApp));
            mAdapter.notifyDataSetChanged();
            setCheckedItems();
        }
    }

    @Override
    protected void onPause() {
        SparseBooleanArray arr = mAppsView.getCheckedItemPositions();
        List<AppInfo> apps = mAdapter.getAppList();
        for (int i = 0; i < arr.size(); i++) {
            if (i >= apps.size()) {
                continue;
            }
            String name = apps.get(i).getPackageName();
            if (arr.get(i)) {
                mConfig.add(name);
            } else {
                mConfig.remove(name);
            }
        }
        ConfigUtils.put(this, mConfig);
        super.onPause();
    }
}
