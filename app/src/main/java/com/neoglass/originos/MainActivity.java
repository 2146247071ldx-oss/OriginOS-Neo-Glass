package com.neoglass.originos;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private View backgroundView;
    private ValueAnimator colorAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);

        // 1. 根布局 (使用 FrameLayout 来实现背景在底，卡片在上的叠加效果)
        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);
        
        // 2. 动态流光背景
        backgroundView = new View(this);
        frameLayout.addView(backgroundView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        startAuroraAnimation();

        // 3. 滚动容器
        ScrollView scrollView = new ScrollView(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(60, 150, 60, 150);
        rootLayout.setGravity(Gravity.CENTER); // 整体居中
        scrollView.addView(rootLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frameLayout.addView(scrollView);

        // 4. 液态玻璃卡片 (带阴影和微光边框)
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(60, 70, 60, 70);
        cardLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        
        GradientDrawable glassDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#4DFFFFFF"), Color.parseColor("#1AFFFFFF")} // 更透的白
        );
        glassDrawable.setCornerRadius(80f);
        glassDrawable.setStroke(2, Color.parseColor("#66FFFFFF"));
        cardLayout.setBackground(glassDrawable);
        
        // 添加卡片阴影 (Android 5.0+)
        cardLayout.setElevation(30f);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLayout.setLayoutParams(cardParams);

        // 5. 标题与排版
        TextView title = new TextView(this);
        title.setText("NEO GLASS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(36f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        cardLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("OriginOS 6 · 半透明玻璃风");
        subtitle.setTextColor(Color.parseColor("#E6FFFFFF"));
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 60);
        cardLayout.addView(subtitle);

        // 6. 添加四个美化开关
        addSwitch(cardLayout, "时钟玻璃美化", "clock");
        addSwitch(cardLayout, "通知界面美化", "notification");
        addSwitch(cardLayout, "控制中心美化", "control");
        addSwitch(cardLayout, "动画效果美化", "animation");

        // 7. 底部作者链接
        TextView author = new TextView(this);
        author.setText("开发预览版 | 酷安: @惩煦"); 
        author.setTextColor(Color.parseColor("#80FFFFFF")); 
        author.setTextSize(13f);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 70, 0, 10);
        author.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/39927079")));
            } catch (Exception ignored) {}
        });
        cardLayout.addView(author);

        rootLayout.addView(cardLayout);
        setContentView(frameLayout);
    }

    // 动态流光背景动画
    private void startAuroraAnimation() {
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(6000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.setInterpolator(new LinearInterpolator());
        
        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            // 动态渐变颜色：从深邃蓝到幽暗紫
            int color1 = blendColors(Color.parseColor("#1A2980"), Color.parseColor("#4B1248"), fraction);
            int color2 = blendColors(Color.parseColor("#26D0CE"), Color.parseColor("#0F2027"), fraction);
            
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color1, color2});
            backgroundView.setBackground(bg);
        });
        colorAnimator.start();
    }

    // 混合颜色计算
    private int blendColors(int color1, int color2, float ratio) {
        float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    // 辅助方法：绘制精致的开关行
    private void addSwitch(LinearLayout parent, String labelText, final String prefKey) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 40, 0, 40);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(labelParams);

        Switch switchView = new Switch(this);
        boolean isChecked = prefs.getBoolean(prefKey, false);
        switchView.setChecked(isChecked);
        
        // 美化原生开关：滑块白色，轨道半透明
        ColorStateList thumbStateList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#26D0CE"), Color.parseColor("#80FFFFFF")}
        );
        ColorStateList trackStateList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#4DFFFFFF"), Color.parseColor("#26FFFFFF")}
        );
        switchView.setThumbTintList(thumbStateList);
        switchView.setTrackTintList(trackStateList);
        
        switchView.setOnCheckedChangeListener((buttonView, checked) -> 
            prefs.edit().putBoolean(prefKey, checked).apply()
        );

        row.addView(label);
        row.addView(switchView);
        parent.addView(row);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (colorAnimator != null) colorAnimator.cancel();
    }
}