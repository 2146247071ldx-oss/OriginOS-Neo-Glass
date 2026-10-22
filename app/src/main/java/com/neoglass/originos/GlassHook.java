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

        // 读取你在 App 里设置的开关状态
        Map<String, ?> prefs = getRemotePreferences("NeoGlassPrefs").getAll();

        // ========== 游戏魔盒解锁 ==========
        if ("com.vivo.gamecube".equals(pkgName)) {
            boolean unlockGameCube = prefs.containsKey("unlock_gamecube") && (Boolean) prefs.get("unlock_gamecube");
            if (!unlockGameCube) {
                log(Log.INFO, TAG, "游戏魔盒总开关未开启，跳过 Hook");
                return;
            }

            log(Log.INFO, TAG, "准备解锁游戏魔盒隐藏功能...");

            // 强行返回 true 的回调
            XposedInterface.Hooker forceTrueHooker = new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    chain.proceed(); // 先让原方法执行
                    return true;     // 然后强制覆盖返回值为 true
                }
            };

            // 性能模式与散热风扇相关类
            hookAllMethods(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.a", "a", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.n", "v", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.n", "m", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift.n", "n", forceTrueHooker);
            
            // 通用功能开关类
            hookAllMethods(cl, "com.vivo.common.utils.w0", "a", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.common.a", "v0", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.common.a", "D1", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.common.a", "R0", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.common.a", "u1", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.common.utils.m0", "g", forceTrueHooker);
            hookAllMethods(cl, "com.vivo.common.utils.m0", "f", forceTrueHooker);

            log(Log.INFO, TAG, "游戏魔盒 Hook 注入完毕！");
        }
    }

    // 辅助方法：避免代码写得太长，防崩溃
    private void hookAllMethods(ClassLoader cl, String className, String methodName, XposedInterface.Hooker hooker) {
        try {
            Class<?> clazz = Class.forName(className, false, cl);
            hookAllMethods(clazz, methodName, hooker);
        } catch (Throwable t) {
            log(Log.WARN, TAG, "找不到类 (正常现象): " + className);
        }
    }
}