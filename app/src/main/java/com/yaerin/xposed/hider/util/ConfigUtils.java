package com.yaerin.xposed.hider.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
    private static SharedPreferences.Editor mPrefEdit;


    public static void put(Context context, Set<String> apps) {

        mPreferences = context.getSharedPreferences("enabled", Activity.MODE_PRIVATE);
        mPrefEdit = mPreferences.edit();
        Gson g = new Gson();
        String js = g.toJson(apps);
        mPrefEdit.putString("apps",js);
        mPrefEdit.apply();
    }

    public static Set<String> get(Context context) {
        String se = "" ;
        StringBuilder s = new StringBuilder();
        mPreferences = context.getSharedPreferences("enabled", Activity.MODE_PRIVATE);
        se =  mPreferences.getString("apps","");
        if(se.isEmpty()) {
            Toast.makeText(context, "get: empty!", Toast.LENGTH_SHORT).show();
            return null;
        }
        Toast.makeText(context,"get: "+se,Toast.LENGTH_SHORT).show();
        try {
            Set<String> ss = new Gson().fromJson(se, new TypeToken<Set<String>>() {
            }.getType());
            return ss;
        }catch (Exception e) {
            return null;
        }
    }
}
