package com.neoglass.originos;

import android.util.Log;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class GlassHook extends XposedModule {

    private static final String TAG = "LSPosed-OriginBreaker";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        super.onModuleLoaded(param);
        log(Log.INFO, TAG, "MODULE_ALIVE: 模块已加载 | process=" + param.getProcessName());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        super.onPackageReady(param);
        
        String pkgName = param.getPackageName();
        ClassLoader cl = param.getClassLoader();
        if (cl == null) return;

        Map<String, ?> prefs = getRemotePreferences("NeoGlassPrefs").getAll();

        // 1. 解锁游戏魔盒
        if ("com.vivo.gamecube".equals(pkgName)) {
            boolean unlockGameCube = prefs.containsKey("unlock_gamecube") && (Boolean) prefs.get("unlock_gamecube");
            if (!unlockGameCube) {
                log(Log.INFO, TAG, "游戏魔盒总开关未开启，跳过 Hook");
                return;
            }

            log(Log.INFO, TAG, "准备解锁游戏魔盒隐藏功能...");

            XposedInterface.Hooker forceTrueHooker = new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    chain.proceed();
                    return true;
                }
            };

            safeHook(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.a", "a", forceTrueHooker);
            safeHook(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.n", "v", forceTrueHooker);
            safeHook(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.n", "m", forceTrueHooker);
            safeHook(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.n", "n", forceTrueHooker);
            
            safeHook(cl, "com.vivo.common.utils.w0", "a", forceTrueHooker);
            safeHook(cl, "com.vivo.common.a", "v0", forceTrueHooker);
            safeHook(cl, "com.vivo.common.a", "D1", forceTrueHooker);
            safeHook(cl, "com.vivo.common.a", "R0", forceTrueHooker);
            safeHook(cl, "com.vivo.common.a", "u1", forceTrueHooker);
            safeHook(cl, "com.vivo.common.utils.m0", "g", forceTrueHooker);
            safeHook(cl, "com.vivo.common.utils.m0", "f", forceTrueHooker);

            log(Log.INFO, TAG, "游戏魔盒 Hook 注入完毕！");
        }

        // 2. 强制解除截屏限制
        boolean screenSnap = prefs.containsKey("screen_snap") && (Boolean) prefs.get("screen_snap");
        if (screenSnap) {
            log(Log.INFO, TAG, "准备解除 [" + pkgName + "] 的截屏限制...");
            try {
                Class<?> windowClass = Class.forName("android.view.Window", false, cl);
                XposedInterface.Hooker removeSecureFlagHooker = new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        int flags = (int) chain.getArg(0);
                        int mask = (int) chain.getArg(1);
                        // WindowManager.LayoutParams.FLAG_SECURE = 0x00002000
                        if ((mask & 0x00002000) != 0) {
                            mask &= ~0x00002000;
                        }
                        // 核心修复：使用 Object[] 传递参数
                        return chain.proceed(new Object[]{flags, mask});
                    }
                };
                
                for (java.lang.reflect.Method method : windowClass.getDeclaredMethods()) {
                    if (method.getName().equals("setFlags") && method.getParameterTypes().length == 2) {
                        hook(method).intercept(removeSecureFlagHooker);
                    }
                }
                log(Log.INFO, TAG, "截屏限制 Hook 注入完毕！");
            } catch (Throwable t) {
                log(Log.WARN, TAG, "截屏 Hook 失败 (系统版本不兼容)");
            }
        }

        // 3. 强制刷新率欺骗 (仅对非模块本身的进程生效)
        String forceRefreshStr = prefs.containsKey("force_refresh_rate") ? (String) prefs.get("force_refresh_rate") : "0";
        if (!"0".equals(forceRefreshStr) && !"com.neoglass.originos".equals(pkgName)) {
            try {
                final float targetHz = Float.parseFloat(forceRefreshStr);
                Class<?> displayClass = Class.forName("android.view.Display", false, cl);
                XposedInterface.Hooker refreshRateHooker = new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        return targetHz;
                    }
                };
                for (java.lang.reflect.Method method : displayClass.getDeclaredMethods()) {
                    if (method.getName().equals("getRefreshRate")) {
                        hook(method).intercept(refreshRateHooker);
                    }
                }
                log(Log.INFO, TAG, "已强制修改 [" + pkgName + "] 的刷新率为 " + targetHz + "Hz");
            } catch (Throwable ignored) {}
        }
    }

    private void safeHook(ClassLoader cl, String className, String methodName, XposedInterface.Hooker hooker) {
        try {
            Class<?> clazz = Class.forName(className, false, cl);
            for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    hook(method).intercept(hooker); 
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "找不到类或方法 (跳过): " + className + "#" + methodName);
        }
    }
}