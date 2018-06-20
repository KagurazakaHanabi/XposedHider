package com.yaerin.xposed.hide.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.yaerin.xposed.hide.util.SQLiteHelper;

import static android.content.UriMatcher.NO_MATCH;
import static com.yaerin.xposed.hide.C.AUTHORITY;
import static com.yaerin.xposed.hide.C.TABLE_APPS;

public class SettingsProvider extends ContentProvider {

    private static final int CODE_APPS = 1;
    private static final UriMatcher mMatcher = new UriMatcher(NO_MATCH);

    static {
        mMatcher.addURI(AUTHORITY, TABLE_APPS, CODE_APPS);
    }

    private SQLiteDatabase mDB;

    @Override
    public boolean onCreate() {
        SQLiteHelper helper = new SQLiteHelper(getContext());
        mDB = helper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return mDB.query(TABLE_APPS, projection, selection, selectionArgs, null, null, sortOrder, null);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "text/text";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        mDB.insert(TABLE_APPS, null, values);
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return mDB.delete(TABLE_APPS, selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return mDB.update(TABLE_APPS, values, selection, selectionArgs);
    }
}
