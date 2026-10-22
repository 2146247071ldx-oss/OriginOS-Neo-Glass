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
        ClassLoader cl = param.getDefaultClassLoader();

        if (!"com.android.systemui".equals(pkgName)) {
            return;
        }

        log(Log.INFO, TAG, "准备 Hook SystemUI 动画倍率...");

        float scale = 1.0f;
        try {
            // 修正：getRemotePreferences 属于 XposedInterface，通过当前模块实例获取
            Map<String, ?> prefs = getRemotePreferences("NeoGlassPrefs").getAll();
            Object raw = prefs.get("anim_scale");
            if (raw != null) {
                scale = Float.parseFloat(raw.toString());
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "读取远程偏好失败，使用默认值 1.0", t);
        }

        try {
            Class<?> clazz = Class.forName("android.animation.ValueAnimator", false, cl);
            Field field = clazz.getDeclaredField("sDurationScale");
            field.setAccessible(true);
            field.setFloat(null, scale);
            log(Log.INFO, TAG, "Hook 成功！已修改动画倍率为: " + scale);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Hook 失败", t);
        }
    }
}