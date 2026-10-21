package com.neoglass.originos;

import android.os.Build;
import android.view.Surface;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;

public class GlassHook implements IXposedHookLoadPackage {

    private static final String TAG = "OriginOS Neo Glass: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 高刷控制涉及系统底层，需要作用于 system_server (android) 以及 SystemUI
        if (!"android".equals(lpparam.packageName) && !"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            XSharedPreferences prefs = new XSharedPreferences("com.neoglass.originos", "NeoGlassPrefs");
            prefs.makeWorldReadable();
            boolean maxRefreshOn = prefs.getBoolean("max_refresh_rate", false);

            if (maxRefreshOn) {
                XposedBridge.log(TAG + "检测到高刷开关已开启，正在注入底层逻辑 -> " + lpparam.packageName);
                
                // 尝试 1：Hook DisplayManager 获取最高支持刷新率
                hookDisplayManager(lpparam);
                
                // 尝试 2：Hook 系统设置中负责限制帧率的方法（针对某些定制 ROM）
                hookSystemFrameLimit(lpparam);
            }

        } catch (Throwable t) {
            XposedBridge.log(TAG + "错误: " + t.getMessage());
        }
    }

    private void hookDisplayManager(LoadPackageParam lpparam) {
        try {
            Class<?> displayManagerClass = XposedHelpers.findClass("android.hardware.display.DisplayManagerGlobal", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(displayManagerClass, "getDisplayMode", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 获取原本的 Display.Mode
                    Object mode = param.getResult();
                    if (mode != null) {
                        Class<?> modeClass = mode.getClass();
                        // 强行将刷新率字段修改为最高
                        try {
                            // 大多数 Android 系统中 refreshRate 字段位于 Display.Mode
                            java.lang.reflect.Field refreshRateField = modeClass.getDeclaredField("mRefreshRate");
                            refreshRateField.setAccessible(true);
                            refreshRateField.setFloat(mode, 144f); // iQOO Neo11 最高支持 144Hz
                        } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook DisplayManager 失败(可能是厂商改名): " + t.getMessage());
        }
    }

    private void hookSystemFrameLimit(LoadPackageParam lpparam) {
        try {
            // 针对 OriginOS 常见的电源管理类进行 Hook
            // 如果找不到类，也不会崩溃，仅记录日志
            Class<?> powerManagerService = XposedHelpers.findClassIfExists("com.android.server.power.PowerManagerService", lpparam.classLoader);
            if (powerManagerService != null) {
                XposedHelpers.findAndHookMethod(powerManagerService, "getDisplayRefreshRate", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return 144f; // 强制返回最高值
                    }
                });
                XposedBridge.log(TAG + "Hook PowerManagerService 成功");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook 系统帧率限制失败: " + t.getMessage());
        }
    }
}