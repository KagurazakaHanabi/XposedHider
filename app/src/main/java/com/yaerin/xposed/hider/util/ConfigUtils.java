package com.yaerin.xposed.hider.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Set;

/**
 * Create by Yaerin on 2018/6/23
 *
 * @author Yaerin
 * @author EBK21
 */
public class ConfigUtils {

    private static SharedPreferences mPreferences;


    private static void writeconfall(Context context,Set<String> apps) {
        SharedPreferences.Editor mPrefEdit;
        mPreferences = context.getSharedPreferences("enabled", Activity.MODE_PRIVATE);
        mPrefEdit = mPreferences.edit();
        Gson g = new Gson();
        String js = g.toJson(apps);
        mPrefEdit.putString("apps",js);
        mPrefEdit.apply();
    }
    public static void put(Context context, Set<String> apps) {
        writeconfall(context,apps);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Context decon = context.createDeviceProtectedStorageContext();
            writeconfall(decon, apps);
        }
    }

    public static Set<String> get(Context context) {
        String se;
        mPreferences = context.getSharedPreferences("enabled", Activity.MODE_PRIVATE);
        se =  mPreferences.getString("apps","null");
        if(se != null && se.equals("null")) {
            Toast.makeText(context, "get: empty!", Toast.LENGTH_SHORT).show();
            return null;
        }
        Toast.makeText(context,"get: "+se,Toast.LENGTH_SHORT).show();
        try {
            return new Gson().fromJson(se, new TypeToken<Set<String>>() {
            }.getType());
        }catch (Exception e) {
            return null;
        }
    }
}
