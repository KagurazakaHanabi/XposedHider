package com.yaerin.xposed.hider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Keep;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import top.fols.box.io.FilterXpInputStream;

/**
 * helpful link: https://github.com/w568w/XposedChecker
 */
@Keep
@SuppressWarnings("unchecked")
public class XposedHook {


    private String mSdcard;




    private static boolean isXposedModule(Context context, ApplicationInfo applicationInfo) {
        Bundle bundle = null;
        try {
            bundle = context.getPackageManager()
                    .getApplicationInfo(applicationInfo.packageName, PackageManager.GET_META_DATA)
                    .metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return bundle != null && bundle.getBoolean("xposedmodule", false);
    }

    @Keep
    public void handleLoadPackage(Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        mSdcard = Environment.getExternalStorageDirectory().getPath();
        if (!isXposedModule(context, lpparam.appInfo)) {
            next(lpparam);
        }
    }

    private void next(XC_LoadPackage.LoadPackageParam lpparam) {

        if (lpparam.classLoader == null)  {
            return;
        }

        XposedBridge.log("[I/XposedHider] Handle package " + lpparam.packageName);

        XC_MethodHook hookClass = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String packageName = (String) param.args[0];
                if (packageName.matches("de\\.robv\\.android\\.xposed\\.Xposed+.+")) {
                    param.setThrowable(new ClassNotFoundException(packageName));
                }
            }
        };
        // FIXME: 18-6-23 w568w: It's very dangerous to hook these methods, thinking to replace them.
        XposedHelpers.findAndHookMethod(
                ClassLoader.class,
                "loadClass",
                String.class,
                boolean.class,
                hookClass
        );
        XposedHelpers.findAndHookMethod(
                Class.class,
                "forName",
                String.class,
                boolean.class,
                ClassLoader.class,
                hookClass
        );

        XposedHelpers.findAndHookConstructor(
                File.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String path = (String) param.args[0];
                        boolean shouldDo = path.matches("/proc/[0-9]+/maps") ||
                                (path.toLowerCase().contains(C.KW_XPOSED) &&
                                        !path.startsWith(mSdcard) && !path.contains("fkzhang"));
                        if (shouldDo) {
                            param.args[0] = "/system/build.prop";
                        }
                    }
                }
        );

        XC_MethodHook hookStack = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                StackTraceElement[] elements = (StackTraceElement[]) param.getResult();
                List<StackTraceElement> clone = new ArrayList<>();
                for (StackTraceElement element : elements) {
                    if (!element.getClassName().toLowerCase().contains(C.KW_XPOSED)) {
                        clone.add(element);
                    }
                }
                param.setResult(clone.toArray(new StackTraceElement[0]));
            }
        };
        XposedHelpers.findAndHookMethod(
                Throwable.class,
                "getStackTrace",
                hookStack
        );
        XposedHelpers.findAndHookMethod(
                Thread.class,
                "getStackTrace",
                hookStack
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
                        List<PackageInfo> clone = new ArrayList<>();
                        // foreach is very slow.
                        final int len = apps.size();
                        for (int i = 0; i < len; i++) {
                            PackageInfo app = apps.get(i);
                            if (!app.packageName.toLowerCase().contains(C.KW_XPOSED)) {
                                clone.add(app);
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
                        List<ApplicationInfo> clone = new ArrayList<>();
                        final int len = apps.size();
                        for (int i = 0; i < len; i++) {
                            ApplicationInfo app = apps.get(i);
                            boolean shouldRemove = app.metaData != null && app.metaData.getBoolean("xposedmodule") ||
                                    app.packageName != null && app.packageName.toLowerCase().contains(C.KW_XPOSED) ||
                                    app.className != null && app.className.toLowerCase().contains(C.KW_XPOSED) ||
                                    app.processName != null && app.processName.toLowerCase().contains(C.KW_XPOSED);
                            if (!shouldRemove) {
                                clone.add(app);
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
                        if ("vxp".equals(param.args[0])) {
                            param.setResult(null);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                File.class,
                "list",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String[] fs = (String[]) param.getResult();
                        if (fs == null) {
                            return;
                        }
                        List<String> list = new ArrayList<>();
                        for (String f : fs) {
                            if (!f.toLowerCase().contains(C.KW_XPOSED) && !f.equals("su")) {
                                list.add(f);
                            }
                        }
                        param.setResult(list.toArray(new String[0]));
                    }
                }
        );

        Class<?> clazz = null;
        try {
            clazz = Runtime.getRuntime().exec("echo").getClass();
        } catch (IOException ignore) {
            XposedBridge.log("[W/XposedHider] Cannot hook Process#getInputStream");
        }
        if (clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getInputStream",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            InputStream is = (InputStream) param.getResult();
                            if (is instanceof FilterXpInputStream) {
                                param.setResult(is);
                            } else {
                                param.setResult(new FilterXpInputStream(is));
                            }
                        }
                    }
            );
        }

        XposedBridge.hookAllMethods(System.class, "getenv",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args.length == 0) {
                            Map<String, String> res = (Map<String, String>) param.getResult();
                            String classpath = res.get("CLASSPATH");
                            param.setResult(filter(classpath));
                        } else if ("CLASSPATH".equals(param.args[0])) {
                            String classpath = (String) param.getResult();
                            param.setResult(filter(classpath));
                        }
                    }

                    private String filter(String s) {
                        List<String> list = Arrays.asList(s.split(":"));
                        List<String> clone = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            if (!list.get(i).toLowerCase().contains(C.KW_XPOSED)) {
                                clone.add(list.get(i));
                            }
                        }
                        StringBuilder res = new StringBuilder();
                        for (int i = 0; i < clone.size(); i++) {
                            res.append(clone);
                            if (i != clone.size() - 1) {
                                res.append(":");
                            }
                        }
                        return res.toString();
                    }
                }
        );
    }
}
