package com.neoglass.originos;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GlassHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("OriginOS Neo Glass: System UI 已加载");

        // 当前仅检测 System UI 加载状态。
        // 时钟、通知、控制中心和动画的实际美化
        // 还需要针对 OriginOS 6 的具体版本适配。
    }
}