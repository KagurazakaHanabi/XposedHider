package com.yaerin.xposed.hide.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.yaerin.xposed.hide.bean.AppInfo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.yaerin.xposed.hide.C.COLUMN_DISABLED;
import static com.yaerin.xposed.hide.C.COLUMN_FLAGS;
import static com.yaerin.xposed.hide.C.COLUMN_ICON;
import static com.yaerin.xposed.hide.C.COLUMN_LABEL;
import static com.yaerin.xposed.hide.C.COLUMN_PACKAGE;
import static com.yaerin.xposed.hide.C.TABLE_APPS;

public class Utilities {

    public static boolean isSystemApp(int flags) {
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public static void putConfig(Context context, List<AppInfo> apps) {
        SQLiteDatabase db = new SQLiteHelper(context).getWritableDatabase();
        String base = "UPDATE " + TABLE_APPS + " SET " + COLUMN_DISABLED + "=";
        StringBuilder where = new StringBuilder("(");
        for (int i = 0; i < apps.size(); i++) {
            where.append("\"").append(apps.get(i).getPackageName()).append("\"");
            if (i != apps.size() - 1) {
                where.append(",");
            }
        }
        db.execSQL(base + 0 + " WHERE " + COLUMN_PACKAGE + " NOT IN " + where + ")");
        db.execSQL(base + 1 + " WHERE " + COLUMN_PACKAGE + " IN " + where + ")");
    }

    public static List<String> getConfig(Context context) {
        Uri uri = Uri.parse("content://com.yaerin.xposed.hide/apps");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        List<String> apps = new ArrayList<>();
        if (cursor == null) {
            return apps;
        }
        while (cursor.moveToNext()) {
            if (cursor.getInt(cursor.getColumnIndex(COLUMN_DISABLED)) == 1) {
                apps.add(cursor.getString(cursor.getColumnIndex("package")));
            }
        }
        cursor.close();
        return apps;
    }

    public static List<AppInfo> getAppList(Context context) {
        SQLiteDatabase db = new SQLiteHelper(context).getReadableDatabase();
        Cursor cursor = db.query(TABLE_APPS, null, null, null, null, null, null, null);
        List<AppInfo> appList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String packageName = cursor.getString(cursor.getColumnIndex(COLUMN_PACKAGE));
            String label = cursor.getString(cursor.getColumnIndex(COLUMN_LABEL));
            int flags = cursor.getInt(cursor.getColumnIndex(COLUMN_FLAGS));
            byte[] bytes = cursor.getBlob(cursor.getColumnIndex(COLUMN_ICON));
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Drawable icon = new BitmapDrawable(context.getResources(), bitmap);
            boolean disabled = cursor.getInt(cursor.getColumnIndex(COLUMN_DISABLED)) == 1;
            appList.add(new AppInfo(packageName, label, flags, icon, disabled));
        }
        cursor.close();
        return appList;
    }

    public static void updateAppList(Context context) {
        SQLiteDatabase db = new SQLiteHelper(context).getWritableDatabase();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<AppInfo> newList = new ArrayList<>();
        for (int i = 0; i < apps.size(); i++) {
            ApplicationInfo app = apps.get(i);
            if (!isSystemApp(app.flags)) {
                String packageName = app.packageName;
                String label = app.loadLabel(pm).toString();
                int flags = app.flags;
                Drawable icon = app.loadIcon(pm);
                boolean disabled = false;

                Cursor cursor = db.rawQuery(
                        String.format("select %s from %s where %s=\"%s\"",
                                COLUMN_DISABLED, TABLE_APPS, COLUMN_PACKAGE, packageName), null);
                if (cursor.getCount() == 1) {
                    cursor.moveToPosition(0);
                    disabled = cursor.getInt(cursor.getColumnIndex(COLUMN_DISABLED)) == 1;
                }
                cursor.close();

                newList.add(new AppInfo(packageName, label, flags, icon, disabled));
            }
        }
        db.execSQL("delete from " + TABLE_APPS);
        for (AppInfo app : newList) {
            Bitmap bitmap = (((BitmapDrawable) app.getIcon()).getBitmap());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] iconBytes = bos.toByteArray();
            ContentValues values = new ContentValues();
            values.put(COLUMN_PACKAGE, app.getPackageName());
            values.put(COLUMN_LABEL, app.getLabel());
            values.put(COLUMN_FLAGS, app.getFlags());
            values.put(COLUMN_ICON, iconBytes);
            values.put(COLUMN_DISABLED, app.isDisabled());
            db.insert(TABLE_APPS, null, values);
        }
    }
}
