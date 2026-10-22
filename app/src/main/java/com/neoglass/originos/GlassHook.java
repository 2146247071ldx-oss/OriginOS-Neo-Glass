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
            if (unlockGameCube) {
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

        // 2. 强制解除截屏限制
        boolean screenSnap = prefs.containsKey("screen_snap") && (Boolean) prefs.get("screen_snap");
        if (screenSnap) {
            try {
                Class<?> windowClass = Class.forName("android.view.Window", false, cl);
                XposedInterface.Hooker removeSecureFlagHooker = new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        int flags = (int) chain.getArg(0);
                        int mask = (int) chain.getArg(1);
                        if ((mask & 0x00002000) != 0) {
                            mask &= ~0x00002000;
                        }
                        return chain.proceed(new Object[]{flags, mask});
                    }
                };
                
                for (java.lang.reflect.Method method : windowClass.getDeclaredMethods()) {
                    if (method.getName().equals("setFlags") && method.getParameterTypes().length == 2) {
                        hook(method).intercept(removeSecureFlagHooker);
                    }
                }
                log(Log.INFO, TAG, "截屏限制 Hook 注入完毕！");
            } catch (Throwable ignored) {}
        }

        // 3. 强制 144Hz 刷新率
        boolean force144Hz = prefs.containsKey("force_144hz") && (Boolean) prefs.get("force_144hz");
        if (force144Hz && !"com.neoglass.originos".equals(pkgName)) {
            try {
                final float targetHz = 144.0f;
                
                Class<?> windowClass = Class.forName("android.view.Window", false, cl);
                Class<?> layoutParamsClass = Class.forName("android.view.WindowManager$LayoutParams", false, cl);
                
                XposedInterface.Hooker windowAttrHooker = new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object layoutParams = chain.getArg(0);
                        if (layoutParams != null && layoutParamsClass.isInstance(layoutParams)) {
                            try {
                                java.lang.reflect.Field field = layoutParamsClass.getField("preferredRefreshRate");
                                field.set(layoutParams, targetHz);
                            } catch (Throwable ignored) {}
                        }
                        return chain.proceed();
                    }
                };
                
                for (java.lang.reflect.Method method : windowClass.getDeclaredMethods()) {
                    if (method.getName().equals("setAttributes") && method.getParameterTypes().length == 1) {
                        hook(method).intercept(windowAttrHooker);
                    }
                }

                Class<?> displayClass = Class.forName("android.view.Display", false, cl);
                XposedInterface.Hooker displayHooker = new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        return targetHz;
                    }
                };
                
                for (java.lang.reflect.Method method : displayClass.getDeclaredMethods()) {
                    if (method.getName().equals("getRefreshRate")) {
                        hook(method).intercept(displayHooker);
                    }
                }

                log(Log.INFO, TAG, "已通过 Hook 强制 [" + pkgName + "] 高刷至 144Hz");
            } catch (Throwable t) {
                log(Log.WARN, TAG, "刷新率 Hook 失败: " + t.getMessage());
            }
        }

        // 4. 强制 IPv4 解析
        boolean forceIPv4 = prefs.containsKey("force_ipv4") && (Boolean) prefs.get("force_ipv4");
        if (forceIPv4 && !"com.neoglass.originos".equals(pkgName)) {
            try {
                Class<?> inetAddressClass = Class.forName("java.net.InetAddress", false, cl);
                XposedInterface.Hooker ipv4Hooker = new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object result = chain.proceed();
                        if (result instanceof java.net.InetAddress[]) {
                            java.net.InetAddress[] addrs = (java.net.InetAddress[]) result;
                            java.util.List<java.net.InetAddress> ipv4List = new java.util.ArrayList<>();
                            for (java.net.InetAddress addr : addrs) {
                                if (addr instanceof java.net.Inet4Address) {
                                    ipv4List.add(addr);
                                }
                            }
                            if (!ipv4List.isEmpty()) {
                                return ipv4List.toArray(new java.net.InetAddress[0]);
                            }
                        }
                        return result;
                    }
                };
                
                for (java.lang.reflect.Method m : inetAddressClass.getDeclaredMethods()) {
                    if (m.getName().equals("getAllByName") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == String.class) {
                        hook(m).intercept(ipv4Hooker);
                    }
                }
                log(Log.INFO, TAG, "已强制 [" + pkgName + "] 过滤 IPv6 解析结果");
            } catch (Throwable t) {
                log(Log.WARN, TAG, "IPv4 Hook 失败: " + t.getMessage());
            }
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
        } catch (Throwable ignored) {}
    }
}