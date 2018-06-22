package com.yaerin.xposed.hider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Keep;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.yaerin.xposed.hider.util.Utilities.getConfig;

/**
 * helpful link: https://github.com/w568w/XposedChecker
 */
@SuppressWarnings("unchecked")
@Keep
public class XposedHook {

    private String mSdcard;

    public static boolean isXposedModule(ApplicationInfo applicationInfo, Context context) {
        Bundle bundle = null;
        try {
            bundle = context.getPackageManager().getApplicationInfo(applicationInfo.packageName, PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return bundle != null && bundle.getBoolean("xposedmodule", false);
    }

    @Keep
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam, Context context) {
        mSdcard = Environment.getExternalStorageDirectory().getPath();
        if (!isXposedModule(lpparam.appInfo, context)) {
            next(context, lpparam);
        }
    }

    private void next(Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.classLoader == null) {
            return;
        }
        if (!getConfig(context).contains(lpparam.packageName)) {
            return;
        }

        XC_MethodHook hookClass = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String packageName = (String) param.args[0];
                // must match C.XPOSED exactly
                if (packageName.contains(C.XPOSED)) {
                    param.setThrowable(new ClassNotFoundException(packageName));
                }
            }
        };
        // FIXME: 18-6-23 w568w:It's very dangerous to hook these methods,thinking to replace them.
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
                                (path.toLowerCase().contains(C.KW_XPOSED) && !path.startsWith(mSdcard));
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
                        //Foreach is very slow.
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

        Class<?> clazz = null;
        try {
            clazz = Class.forName("java.lang.ProcessManager$ProcessImpl");
        } catch (ClassNotFoundException ignore) {
            try {
                clazz = Class.forName("java.lang.ProcessImpl");
            } catch (ClassNotFoundException e) {
                XposedBridge.log("[W] Can't hook Process#getInputStream");
            }
        }
        if (clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getInputStream",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            InputStream is = (InputStream) param.getResult();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            StringBuilder s = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.toLowerCase()
                                        .contains(C.KW_XPOSED) && !"su".equals(line)) {
                                    s.append(line).append("\\n");
                                }
                            }
                            param.setResult(new ByteArrayInputStream(s.toString().getBytes()));
                        }
                    }
            );
        }
    }
}
