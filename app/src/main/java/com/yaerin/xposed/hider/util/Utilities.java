package com.yaerin.xposed.hider.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.yaerin.xposed.hider.bean.AppInfo;

import java.util.ArrayList;
import java.util.List;


public class Utilities {

    public static boolean isSystemApp(int flags) {
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public static List<AppInfo> getAppList(Context context, boolean sys) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<AppInfo> newList = new ArrayList<>();
        for (int i = 0; i < apps.size(); i++) {
            ApplicationInfo info = apps.get(i);
            if (!isSystemApp(info.flags) || sys) {
                newList.add(new AppInfo(
                        info.packageName, info.loadLabel(pm).toString(), info.flags, info.loadIcon(pm)
                ));
            }
        }
        return newList;
    }
}
