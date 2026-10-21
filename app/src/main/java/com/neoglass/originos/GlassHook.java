package com.neoglass.originos;

import android.animation.TimeInterpolator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;

public class GlassHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 只针对系统界面
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            XSharedPreferences prefs = new XSharedPreferences("com.neoglass.originos", "NeoGlassPrefs");
            prefs.makeWorldReadable();
            
            boolean springOn = prefs.getBoolean("spring_anim", false);
            boolean tickerOn = prefs.getBoolean("ticker_on", false);
            String tickerText = prefs.getString("ticker_text", "Origin Cyber");

            if (springOn) {
                hookSpringAnimation(lpparam);
            }
            if (tickerOn && !TextUtils.isEmpty(tickerText)) {
                hookTickerText(lpparam, tickerText);
            }

        } catch (Throwable t) {
            XposedBridge.log("Origin Cyber 错误: " + t.getMessage());
        }
    }

    // 1. 全局 Q 弹弹簧动画
    private void hookSpringAnimation(LoadPackageParam lpparam) {
        try {
            // 替换系统的默认插值器，强行改成带有回弹效果的 OvershootInterpolator
            XposedHelpers.findAndHookMethod("android.view.animation.AnimationUtils", lpparam.classLoader, "loadInterpolator", 
                    android.content.Context.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 拦截标准的线性/减速插值器，替换成弹簧
                    Object result = param.getResult();
                    if (result != null && result.getClass().getName().contains("LinearInterpolator")) {
                        param.setResult(new OvershootInterpolator(1.2f));
                    }
                }
            });
            XposedBridge.log("Origin Cyber: 弹簧引擎已注入");
        } catch (Throwable ignored) {}
    }

    // 2. 状态栏赛博弹幕
    private void hookTickerText(LoadPackageParam lpparam, String text) {
        try {
            // 寻找状态栏的根视图
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ViewGroup statusBarView = (ViewGroup) param.thisObject;
                    
                    // 动态创建一个跑马灯 TextView
                    TextView ticker = new TextView(statusBarView.getContext());
                    ticker.setText(text);
                    ticker.setTextColor(Color.parseColor("#26D0CE")); // 赛博青色
                    ticker.setTextSize(12f);
                    ticker.setTypeface(Typeface.DEFAULT_BOLD);
                    ticker.setSingleLine(true);
                    ticker.setEllipsize(TextUtils.TruncateAt.MARQUEE); // 设为滚动
                    ticker.setMarqueeRepeatLimit(-1); // 无限循环
                    ticker.setSelected(true); // 激活滚动
                    
                    // 把跑马灯加到状态栏最左侧
                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    statusBarView.addView(ticker, 0, lp);
                    
                    XposedBridge.log("Origin Cyber: 赛博弹幕已插入状态栏");
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("Origin Cyber: 弹幕注入失败 -> " + t.getMessage());
        }
    }
}