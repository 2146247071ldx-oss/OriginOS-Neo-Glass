package com.neoglass.originos;

import android.os.IBinder;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;

public class GlassHook implements IXposedHookLoadPackage {

    private XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        
        // 【核心修复】纯 LSPosed 拦截，一旦模块生效，强改 App 状态为已激活
        if ("com.neoglass.originos".equals(lpparam.packageName)) {
            try {
                XposedHelpers.findAndHookMethod("com.neoglass.originos.MainActivity", lpparam.classLoader, 
                    "isModuleActive", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return true; 
                        }
                    });
            } catch (Throwable ignored) {}
            return; 
        }

        prefs = new XSharedPreferences("com.neoglass.originos", "NeoGlassPrefs");
        prefs.makeWorldReadable();

        try {
            hookAnimationScale(lpparam);

            if (prefs.getBoolean("screen_snap", false)) {
                hookScreenSnap(lpparam);
            }

            if ("android".equals(lpparam.packageName)) {
                if (prefs.getBoolean("thermal_spoof", false)) {
                    String tempStr = prefs.getString("thermal_temp", "25.0");
                    float fakeTemp = 25.0f;
                    try { fakeTemp = Float.parseFloat(tempStr); } catch (Exception ignored) {}
                    hookThermalSpoof(lpparam, fakeTemp);
                }
                if (prefs.getBoolean("limit_charge", false)) hookChargeLimit(lpparam);
                if (prefs.getBoolean("wakelock_killer", false)) hookWakelock(lpparam);
            }
        } catch (Throwable t) {
            XposedBridge.log("OriginBreaker: " + t.getMessage());
        }
    }

    private void hookAnimationScale(LoadPackageParam lpparam) {
        try {
            String animScaleStr = prefs.getString("anim_scale", "1.0");
            final float animScale = Float.parseFloat(animScaleStr);
            if (animScale == 1.0f) return; 

            XposedHelpers.findAndHookMethod("android.provider.Settings$Global", lpparam.classLoader, "getFloat", 
                    android.content.ContentResolver.class, String.class, float.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String settingName = (String) param.args[1];
                    if ("window_animation_scale".equals(settingName) ||
                        "transition_animation_scale".equals(settingName) ||
                        "animator_duration_scale".equals(settingName)) {
                        param.setResult(animScale);
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookScreenSnap(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.view.Window", lpparam.classLoader, "addFlags", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int flags = (int) param.args[0];
                    param.args[0] = flags & ~WindowManager.LayoutParams.FLAG_SECURE;
                }
            });
            XposedHelpers.findAndHookMethod("android.view.Window", lpparam.classLoader, "setFlags", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int flags = (int) param.args[0];
                    int mask = (int) param.args[1];
                    param.args[0] = flags & ~WindowManager.LayoutParams.FLAG_SECURE;
                    param.args[1] = mask & ~WindowManager.LayoutParams.FLAG_SECURE;
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookThermalSpoof(LoadPackageParam lpparam, final float fakeTemp) {
        try {
            Class<?> thermalClass = XposedHelpers.findClassIfExists("com.android.server.power.ThermalManagerService", lpparam.classLoader);
            if (thermalClass != null) {
                XposedHelpers.findAndHookMethod(thermalClass, "getCurrentTemperature", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(fakeTemp); 
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    private void hookChargeLimit(LoadPackageParam lpparam) {
        try {
            Class<?> batteryService = XposedHelpers.findClass("com.android.server.BatteryService", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(batteryService, "processValuesLocked", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int level = XposedHelpers.getIntField(param.thisObject, "mBatteryLevel");
                    if (level >= 80) {
                        try { XposedHelpers.setIntField(param.thisObject, "mPlugType", 0); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookWakelock(LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerService = XposedHelpers.findClass("com.android.server.power.PowerManagerService", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(powerManagerService, "acquireWakeLockInternal", 
                    IBinder.class, int.class, String.class, String.class, android.os.WorkSource.class, String.class, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String packageName = (String) param.args[3];
                    if (packageName != null && !packageName.equals("android") && !packageName.contains("systemui")) {
                        param.setResult(null); 
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}