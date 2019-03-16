package com.yaerin.xposed.hider;

import com.crossbowffs.remotepreferences.RemotePreferenceFile;
import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class ConfigProvider extends RemotePreferenceProvider {
    public ConfigProvider() {
        super(BuildConfig.APPLICATION_ID+"r.configs", new RemotePreferenceFile[]{new RemotePreferenceFile("enabled",true)});
    }
    @Override
    protected boolean checkAccess(String prefFileName, String prefKey, boolean write) {
        return !write;
    }
}
