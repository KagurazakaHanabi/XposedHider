package com.yaerin.xposed.hider.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.yaerin.xposed.hider.bean.AppInfo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.yaerin.xposed.hider.C.COLUMN_FLAGS;
import static com.yaerin.xposed.hider.C.COLUMN_ICON;
import static com.yaerin.xposed.hider.C.COLUMN_LABEL;
import static com.yaerin.xposed.hider.C.COLUMN_PACKAGE;
import static com.yaerin.xposed.hider.C.TABLE_APPS;

public class Utilities {

    public static boolean isSystemApp(int flags) {
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public static List<AppInfo> getAppList(Context context, boolean sys) {
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
            if (!isSystemApp(flags) || sys) {
                appList.add(new AppInfo(packageName, label, flags, icon));
            }
        }
        cursor.close();
        return appList;
    }

    public static List<AppInfo> updateAppList(Context context) {
        SQLiteDatabase db = new SQLiteHelper(context).getWritableDatabase();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<AppInfo> newList = new ArrayList<>();
        for (int i = 0; i < apps.size(); i++) {
            ApplicationInfo app = apps.get(i);
            String packageName = app.packageName;
            String label = app.loadLabel(pm).toString();
            int flags = app.flags;
            Drawable icon = app.loadIcon(pm);
            newList.add(new AppInfo(packageName, label, flags, icon));
        }
        db.execSQL("delete from " + TABLE_APPS);
        for (AppInfo app : newList) {
            Bitmap bitmap = getBitmap(app.getIcon());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] iconBytes = bos.toByteArray();
            ContentValues values = new ContentValues();
            values.put(COLUMN_PACKAGE, app.getPackageName());
            values.put(COLUMN_LABEL, app.getLabel());
            values.put(COLUMN_FLAGS, app.getFlags());
            values.put(COLUMN_ICON, iconBytes);
            db.insert(TABLE_APPS, null, values);
        }
        return newList;
    }

    private static Bitmap getBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
}
