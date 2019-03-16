package com.yaerin.xposed.hider;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Keep;

import com.crossbowffs.remotepreferences.RemotePreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by w568w on 18-3-2.
 * <p>
 * So we do not need to reboot, but it'll run slower.
 *
 * @author w568w
 * @author shuihuadx
 * @author EBK21
 */
@Keep
public class InitInjector implements IXposedHookLoadPackage {
    public InitInjector() {
        super();
    }

    private Set<String> getconf(ClassLoader cl,String pkg) {
        Context UseContext;
        Context systemContext = (Context) XposedHelpers.callMethod( XposedHelpers.callStaticMethod( XposedHelpers.findClass("android.app.ActivityThread", cl), "currentActivityThread"), "getSystemContext" );
        try {
            UseContext = systemContext.createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY);
        }catch (Exception er) {
            UseContext = systemContext;
        }
        Context realContext;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            realContext=UseContext.createDeviceProtectedStorageContext();
        }else{
            realContext = UseContext;
        }
        SharedPreferences prefs = new RemotePreferences(realContext, BuildConfig.APPLICATION_ID+"r.configs", "enabled");
        String con;
        Set<String> tp = new HashSet<>();
        con = prefs.getString("apps","null");
        if(con.equals("null")) {
            tp.add("null");
            return tp;
        }
        Gson g = new Gson();
        tp = g.fromJson(con, new TypeToken<Set<String>>() {
        }.getType());
        return tp;
    }

    @Override
    @Keep
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam)  {
        if (loadPackageParam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(
                    "com.yaerin.xposed.hider.ui.MainActivity", loadPackageParam.classLoader,
                    "isEnabled", XC_MethodReplacement.returnConstant(true)
            );
        }
        if(getconf(loadPackageParam.classLoader,loadPackageParam.packageName).contains(loadPackageParam.packageName)) {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Context context = (Context) param.args[0];
                    if (context != null) {
                        loadPackageParam.classLoader = context.getClassLoader();
                        try {
                            invokeHandleHookMethod(
                                    context, BuildConfig.APPLICATION_ID,
                                    BuildConfig.APPLICATION_ID + "r.XposedHook",
                                    "handleLoadPackage", loadPackageParam);
                        } catch (Throwable error) {
                            error.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void invokeHandleHookMethod(
            Context context,
            String modulePackageName,
            String handleHookClass,
            String handleHookMethod,
            XC_LoadPackage.LoadPackageParam loadPackageParam
    ) throws Throwable {
        // 原来的两种方式不是很好,改用这种新的方式
        File apkFile = findApkFile(context, modulePackageName);
        if (apkFile == null) {
            throw new RuntimeException("Cannot find the module APK.");
        }
        // 加载指定的hook逻辑处理类，并调用它的handleHook方法
        PathClassLoader pathClassLoader =
                new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
        Class<?> cls = Class.forName(handleHookClass, true, pathClassLoader);
        Object instance = cls.newInstance();
        Method method = cls.getDeclaredMethod(handleHookMethod,
                Context.class, XC_LoadPackage.LoadPackageParam.class);
        method.invoke(instance, context, loadPackageParam);
    }

    /**
     * 根据包名构建目标Context,并调用getPackageCodePath()来定位apk
     *
     * @param context           context参数
     * @param modulePackageName 当前模块包名
     * @return return apk file
     */
    private File findApkFile(Context context, String modulePackageName) {
        if (context == null) {
            return null;
        }
        try {
            Context moduleContext = context.createPackageContext(
                    modulePackageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            String apkPath = moduleContext.getPackageCodePath();
            return new File(apkPath);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
