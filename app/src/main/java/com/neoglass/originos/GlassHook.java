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