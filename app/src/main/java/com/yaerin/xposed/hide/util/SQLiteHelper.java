package com.yaerin.xposed.hide.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.yaerin.xposed.hide.C.COLUMN_DISABLED;
import static com.yaerin.xposed.hide.C.COLUMN_FLAGS;
import static com.yaerin.xposed.hide.C.COLUMN_ICON;
import static com.yaerin.xposed.hide.C.COLUMN_LABEL;
import static com.yaerin.xposed.hide.C.COLUMN_PACKAGE;
import static com.yaerin.xposed.hide.C.TABLE_APPS;

public class SQLiteHelper extends SQLiteOpenHelper {

    public SQLiteHelper(Context context) {
        super(context, "data.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_APPS + "(" +
                COLUMN_PACKAGE + "  TEXT PRIMARY KEY NOT NULL," +
                COLUMN_LABEL + "    TEXT," +
                COLUMN_FLAGS + "    INT," +
                COLUMN_ICON + "     BLOB," +
                COLUMN_DISABLED + " INT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}
