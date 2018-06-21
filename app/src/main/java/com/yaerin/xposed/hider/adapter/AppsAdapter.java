package com.yaerin.xposed.hider.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.yaerin.xposed.hider.bean.AppInfo;
import com.yaerin.xposed.hider.widget.AppView;

import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends BaseAdapter {

    private Context mContext;
    private List<AppInfo> mApps;

    public AppsAdapter(Context context, List<AppInfo> apps) {
        mContext = context;
        mApps = apps == null ? new ArrayList<>() : apps;
    }

    @Override
    public int getCount() {
        return mApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return new AppView(mContext, mApps.get(position));
    }

    public void add(AppInfo app) {
        mApps.add(app);
    }

    public void remove(int index) {
        mApps.remove(index);
    }

    public List<AppInfo> getAppList() {
        return mApps;
    }

    public void setAppList(List<AppInfo> apps) {
        mApps = apps;
    }
}
