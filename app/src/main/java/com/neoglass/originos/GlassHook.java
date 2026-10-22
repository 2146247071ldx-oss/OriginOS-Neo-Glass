package com.neoglass.originos;

import android.util.Log;
import java.lang.reflect.Field;
import java.util.Map;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class GlassHook extends XposedModule {

    private static final String TAG = "LSPosed-OriginBreaker";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        super.onModuleLoaded(param);
        log(Log.INFO, TAG, "MODULE_ALIVE: OriginBreaker 已加载 | process=" + param.getProcessName());
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        super.onPackageLoaded(param);
        
        String pkgName = param.getPackageName();
        ClassLoader cl = param.getClassLoader();
        
        // 我们暂时只针对系统界面（SystemUI）进行 Hook 测试
        // 因为动画倍率通常在这里体现最明显
        if ("com.android.systemui".equals(pkgName)) {
            log(Log.INFO, TAG, "准备 Hook SystemUI 动画倍率...");
            
            // 读取你在 App 里保存的设置，默认是 1.0
            Map<String, ?> prefs = getRemotePreferences("NeoGlassPrefs").getAll();
            String scaleStr = (String) prefs.get("anim_scale");
            if (scaleStr == null) scaleStr = "1.0";
            
            try {
                float scale = Float.parseFloat(scaleStr);
                
                // 通过反射修改 ValueAnimator 的静态变量 sDurationScale
                Class<?> clazz = Class.forName("android.animation.ValueAnimator", false, cl);
                Field field = clazz.getDeclaredField("sDurationScale");
                field.setAccessible(true);
                field.setFloat(null, scale); // 修改为你的设置值
                
                log(Log.INFO, TAG, "Hook 成功！已修改动画倍率为: " + scale);
            } catch (Throwable t) {
                log(Log.ERROR, TAG, "Hook 失败", t);
            }
        }
    }
}