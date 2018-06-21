package com.yaerin.xposed.hide.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Crashlytics implements Thread.UncaughtExceptionHandler {

    private Context mContext;

    public Crashlytics(Context context) {
        this.mContext = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        String crashTime =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                        .format(new Date());
        String env =
                "########RuntimeEnviormentInormation#######\n" +
                        "crashTime = " + crashTime + "\n" +
                        "model = " + Build.MODEL + "\n" +
                        "android = " + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")\n" +
                        "brand = " + Build.BRAND + "\n" +
                        "manufacturer = " + Build.MANUFACTURER + "\n" +
                        "board = " + Build.BOARD + "\n" +
                        "hardware = " + Build.HARDWARE + "\n" +
                        "device = " + Build.DEVICE + "\n" +
                        "version = " + getVersionName() + "(" + getVersionCode() + ")\n" +
                        "supportAbis = " + getSupportAbis() + "\n" +
                        "display = " + Build.DISPLAY + "\n";
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String stack = "############ForceCloseCrashLog############\n" + writer.toString();
        String message = env + stack;
        try {
            String name = "error_log_" + crashTime + ".log";
            FileOutputStream fos =
                    new FileOutputStream(new File(mContext.getExternalFilesDir("logs"), name));
            fos.write(message.getBytes());
            fos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Process.killProcess(Process.myPid());
        System.exit(1);
    }

    private String getVersionName() {
        String versionName = "unknown";
        try {
            versionName = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    private int getVersionCode() {
        int versionCode = -1;
        try {
            versionCode = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    private String getSupportAbis() {
        String[] abis = Build.SUPPORTED_ABIS;
        StringBuilder abi = new StringBuilder();
        for (int i = 0; i < abis.length; i++) {
            if (i == 0) {
                abi.append(abis[i]);
            } else {
                abi.append(" & ").append(abis[i]);
            }
        }
        return abi.toString();
    }
}
