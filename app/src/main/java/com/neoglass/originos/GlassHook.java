package com.neoglass.originos;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;

public class GlassHook implements IXposedHookLoadPackage {

    private static final String TAG = "OriginEngine: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 排除自身
        if ("com.neoglass.originos".equals(lpparam.packageName)) return;

        try {
            XSharedPreferences prefs = new XSharedPreferences("com.neoglass.originos", "NeoGlassPrefs");
            prefs.makeWorldReadable();

            // === 针对系统框架(android) 的底层 Hook ===
            if ("android".equals(lpparam.packageName)) {
                if (prefs.getBoolean("limit_charge", false)) hookChargeLimit(lpparam);
                if (prefs.getBoolean("wakelock_killer", false)) hookWakelock(lpparam);
            }

            // === 针对系统界面(SystemUI) 的美化 Hook ===
            if ("com.android.systemui".equals(lpparam.packageName)) {
                if (prefs.getBoolean("spring_anim", false)) hookSpringAnimation(lpparam);
                if (prefs.getBoolean("glass_notification", false)) hookNotificationGlass(lpparam);
            }

        } catch (Throwable t) {
            XposedBridge.log(TAG + "错误: " + t.getMessage());
        }
    }

    // ================= 美化模块 =================

    // 1. 全局 Q 弹动画
    private void hookSpringAnimation(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.view.animation.AnimationUtils", lpparam.classLoader, "loadInterpolator", 
                    android.content.Context.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    if (result != null && result.getClass().getName().contains("LinearInterpolator")) {
                        param.setResult(new OvershootInterpolator(1.2f));
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    // 2. 通知卡片毛玻璃化 (强行修改 SystemUI 通知堆栈的背景)
    private void hookNotificationGlass(LoadPackageParam lpparam) {
        try {
            // Hook 通知栏的根容器，强行刷上半透明的深色圆角背景
            Class<?> stackScrollLayout = XposedHelpers.findClassIfExists(
                "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", lpparam.classLoader);
            
            if (stackScrollLayout != null) {
                XposedHelpers.findAndHookMethod(stackScrollLayout, "onAttachedToWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup view = (ViewGroup) param.thisObject;
                        GradientDrawable glassBg = new GradientDrawable();
                        glassBg.setColor(Color.parseColor("#B3101519")); // 70% 不透明的极夜黑
                        glassBg.setCornerRadius(60f);
                        
                        // 强制替换背景，把原本纯色的背景变成圆角玻璃
                        view.setBackground(glassBg);
                    }
                });
                XposedBridge.log(TAG + "通知堆栈毛玻璃已应用");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "通知美化失败(可能是类名被改): " + t.getMessage());
        }
    }

    // ================= 电池模块 =================

    private void hookChargeLimit(LoadPackageParam lpparam) {
        try {
            Class<?> batteryService = XposedHelpers.findClass("com.android.server.BatteryService", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(batteryService, "processValuesLocked", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int level = XposedHelpers.getIntField(param.thisObject, "mBatteryLevel");
                    if (level >= 80) {
                        try {
                            XposedHelpers.setIntField(param.thisObject, "mPlugType", 0); 
                        } catch (Throwable ignored) {}
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
                        param.setResult(null); // 强行拦截流氓唤醒
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}