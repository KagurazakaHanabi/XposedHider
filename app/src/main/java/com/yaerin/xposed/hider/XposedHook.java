package com.yaerin.xposed.hider;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.yaerin.xposed.hider.util.Utilities.getConfig;

/**
 * helpful link: https://github.com/w568w/XposedChecker
 */
@SuppressWarnings("unchecked")
public class XposedHook implements IXposedHookLoadPackage {

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
                    "com.yaerin.xposed.hider.ui.MainActivity", lpparam.classLoader,
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
                // must match C.XPOSED exactly
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
                                (path.toLowerCase().contains(C.KW_XPOSED) && !path.startsWith(mSdcard))) {
                            param.args[0] = "/system/build.prop";
                        }
                    }
                }
        );

        XC_MethodHook HOOK_STACK = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                StackTraceElement[] elements = (StackTraceElement[]) param.getResult();
                List<StackTraceElement> clone = new ArrayList<>(Arrays.asList(elements));
                for (StackTraceElement element : elements) {
                    if (element.getClassName().toLowerCase().contains(C.KW_XPOSED)) {
                        clone.remove(element);
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
                        for (PackageInfo app : apps) {
                            if (app.packageName.toLowerCase().contains(C.KW_XPOSED)) {
                                clone.remove(app);
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
                            if (app.metaData != null && app.metaData.getBoolean("xposedmodule") ||
                                    app.packageName != null && app.packageName.toLowerCase().contains(C.KW_XPOSED) ||
                                    app.className != null && app.className.toLowerCase().contains(C.KW_XPOSED) ||
                                    app.processName != null && app.processName.toLowerCase().contains(C.KW_XPOSED)) {
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

        XposedHelpers.findAndHookMethod(
                Runtime.class,
                "exec",
                String[].class,
                String[].class,
                File.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String[] cmdarray = (String[]) param.args[0];
                        for (String cmd : cmdarray) {
                            if (cmd.toLowerCase().contains(C.KW_XPOSED)) {
                                param.setThrowable(new IOException());
                            }
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
                                        .contains(C.KW_XPOSED) && !line.equals("su")) {
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
