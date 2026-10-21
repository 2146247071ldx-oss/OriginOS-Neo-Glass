package com.neoglass.originos;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;

public class GlassHook implements IXposedHookLoadPackage {

    private static final String TAG = "SensorShield: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 排除自身和系统核心，防止转屏失效
        if ("com.neoglass.originos".equals(lpparam.packageName) || 
            "android".equals(lpparam.packageName)) {
            return;
        }

        try {
            XSharedPreferences prefs = new XSharedPreferences("com.neoglass.originos", "NeoGlassPrefs");
            prefs.makeWorldReadable();
            boolean shieldOn = prefs.getBoolean("sensor_shield", false);

            if (shieldOn) {
                hookSensorEvent(lpparam);
            }

        } catch (Throwable t) {
            XposedBridge.log(TAG + "错误: " + t.getMessage());
        }
    }

    private void hookSensorEvent(LoadPackageParam lpparam) {
        try {
            // 寻找底层的传感器事件队列
            Class<?> sensorQueueClass = XposedHelpers.findClassIfExists("android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader);
            if (sensorQueueClass != null) {
                XposedHelpers.findAndHookMethod(sensorQueueClass, "dispatchSensorEvent", 
                        int.class, float[].class, int.class, long.class, 
                        new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        float[] values = (float[]) param.args[1];
                        // 只要数据不为空，就直接把数值抹平（变成 0）
                        if (values != null) {
                            for (int i = 0; i < values.length; i++) {
                                values[i] = 0f;
                            }
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}
    }
}