package com.yaerin.xposed.hide;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.yaerin.xposed.hide.util.Utilities.getConfig;

/**
 * helpful link: https://github.com/w568w/XposedChecker
 */
@SuppressWarnings("unchecked")
public class XposedHook implements IXposedHookLoadPackage {

    private static final String TAG = "XposedHook";

    private String mSdcard;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        mSdcard = Environment.getExternalStorageDirectory().getPath();
                        next(context, lpparam);
                    }
                }
        );

        if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(
                    "com.yaerin.xposed.hide.ui.MainActivity", lpparam.classLoader,
                    "isEnabled", XC_MethodReplacement.returnConstant(true)
            );
        }
    }

    private void next(Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        if (!getConfig(context).contains(lpparam.packageName)) {
            return;
        }

        XC_MethodHook HOOK_CLASS = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String packageName = (String) param.args[0];
                if (packageName.contains(C.XPOSED)) {
                    param.setThrowable(new ClassNotFoundException(packageName));
                }
            }
        };
        XposedHelpers.findAndHookMethod(
                ClassLoader.class,
                "loadClass",
                String.class,
                boolean.class,
                HOOK_CLASS
        );
        XposedHelpers.findAndHookMethod(
                Class.class,
                "forName",
                String.class,
                boolean.class,
                ClassLoader.class,
                HOOK_CLASS
        );

        XposedHelpers.findAndHookConstructor(
                File.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String path = (String) param.args[0];
                        if (path.matches("/proc/[0-9]+/maps") ||
                                (path.contains(C.KW_XPOSED) && !path.startsWith(mSdcard))) {
                            param.args[0] = "X://Windows";
                        }
                    }
                }
        );

        XC_MethodHook HOOK_STACK = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                StackTraceElement[] elements = (StackTraceElement[]) param.getResult();
                List<StackTraceElement> clone = new ArrayList<>(Arrays.asList(elements));
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i].getClassName().contains(C.XPOSED)) {
                        clone.remove(i);
                    }
                }
                param.setResult(clone.toArray(new StackTraceElement[0]));
            }
        };
        XposedHelpers.findAndHookMethod(
                Throwable.class,
                "getStackTrace",
                HOOK_STACK
        );
        XposedHelpers.findAndHookMethod(
                Thread.class,
                "getStackTrace",
                HOOK_STACK
        );

        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledPackages",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        List<PackageInfo> apps = (List<PackageInfo>) param.getResult();
                        List<PackageInfo> clone = new ArrayList<>(apps);
                        for (int i = 0; i < apps.size(); i++) {
                            PackageInfo app = apps.get(i);
                            if (app.packageName.contains(C.XPOSED)) {
                                clone.remove(i);
                            }
                        }
                        param.setResult(clone);
                    }
                }
        );
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledApplications",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        List<ApplicationInfo> apps = (List<ApplicationInfo>) param.getResult();
                        List<ApplicationInfo> clone = new ArrayList<>(apps);
                        for (int i = 0; i < apps.size(); i++) {
                            ApplicationInfo app = apps.get(i);
                            if (app.metaData.getBoolean("xposedmodule") ||
                                    app.packageName.contains(C.XPOSED) ||
                                    app.className.contains(C.XPOSED) ||
                                    app.processName.contains(C.XPOSED)) {
                                clone.remove(i);
                            }
                        }
                        param.setResult(clone);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                Modifier.class,
                "isNative",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                System.class,
                "getProperty",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args[0].equals("vxp")) {
                            param.setResult(null);
                        }
                    }
                }
        );
    }
}
