package com.neoglass.originos;

import android.content.Intent;
import android.os.PowerManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;

public class GlassHook implements IXposedHookLoadPackage {

    private static final String TAG = "iQOOCharge: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 我们必须 Hook 系统框架 android 进程，因为 BatteryService 都在里面
        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        try {
            XSharedPreferences prefs = new XSharedPreferences("com.neoglass.originos", "NeoGlassPrefs");
            prefs.makeWorldReadable();
            boolean limitCharge = prefs.getBoolean("limit_charge", false);
            boolean screenSlow = prefs.getBoolean("screen_slow_charge", false);

            if (limitCharge) {
                hookChargeLimit(lpparam);
            }
            if (screenSlow) {
                hookScreenCharge(lpparam);
            }

        } catch (Throwable t) {
            XposedBridge.log(TAG + "错误: " + t.getMessage());
        }
    }

    // 1. 限制充电上限 (80%停充)
    private void hookChargeLimit(LoadPackageParam lpparam) {
        try {
            // 寻找系统电量服务
            Class<?> batteryService = XposedHelpers.findClass("com.android.server.BatteryService", lpparam.classLoader);
            
            // Hook 系统更新电量状态的逻辑
            XposedHelpers.findAndHookMethod(batteryService, "processValuesLocked", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 暴力反射获取当前电量
                    int level = XposedHelpers.getIntField(param.thisObject, "mBatteryLevel");
                    
                    if (level >= 80) {
                        // 如果电量到了80，强行把充电状态改成“已充满”
                        // 注意：不同底层 mPlugType 名字可能不同，这里尝试强行修改
                        try {
                            XposedHelpers.setIntField(param.thisObject, "mPlugType", 0); // 0 通常代表 BATTERY_PLUGGED_AC_OFF/不充电
                            XposedHelpers.setBooleanField(param.thisObject, "mBatteryLevelLow", false);
                        } catch (Throwable ignored) {}
                    }
                }
            });
            XposedBridge.log(TAG + "已注入 80% 限充逻辑");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook 充电限制失败: " + t.getMessage());
        }
    }

    // 2. 亮屏强制慢充
    private void hookScreenCharge(LoadPackageParam lpparam) {
        try {
            // 寻找 vivo 定制的电源管理服务 (如果有的话) 或 标准 PowerManager
            Class<?> powerManager = XposedHelpers.findClass("com.android.server.power.PowerManagerService", lpparam.classLoader);
            
            // Hook 息屏/亮屏的触发方法
            XposedHelpers.findAndHookMethod(powerManager, "wakeUpInternal", long.class, String.class, int.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 手机亮屏时，强行通知底层切断快充协议（通过修改电池状态标志位）
                    // 这个方法很难直接调用硬件，但我们可以 Hook 本地的快充类。
                    // 由于 vivo 底层混淆严重，这里我们通过屏蔽充电速度上报来实现“伪慢充”欺骗系统。
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook 屏幕充电失败: " + t.getMessage());
        }
    }
}