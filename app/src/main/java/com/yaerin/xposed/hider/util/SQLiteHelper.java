package com.yaerin.xposed.hider.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.yaerin.xposed.hider.C.COLUMN_FLAGS;
import static com.yaerin.xposed.hider.C.COLUMN_ICON;
import static com.yaerin.xposed.hider.C.COLUMN_LABEL;
import static com.yaerin.xposed.hider.C.COLUMN_PACKAGE;
import static com.yaerin.xposed.hider.C.TABLE_APPS;

public class SQLiteHelper extends SQLiteOpenHelper {

    public SQLiteHelper(Context context) {
        super(context, "data.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_APPS + "(" +
                COLUMN_PACKAGE + "  TEXT PRIMARY KEY NOT NULL," +
                COLUMN_LABEL + "    TEXT," +
                COLUMN_FLAGS + "    INT," +
                COLUMN_ICON + "     BLOB)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            db.execSQL("drop table " + TABLE_APPS);
            onCreate(db);
        }
    }


}
