package com.neoglass.originos;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private View backgroundView;
    private ValueAnimator colorAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_WORLD_READABLE);

        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);

        backgroundView = new View(this);
        frameLayout.addView(backgroundView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        startAuroraAnimation();

        ScrollView scrollView = new ScrollView(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(60, 150, 60, 150);
        rootLayout.setGravity(Gravity.CENTER);
        scrollView.addView(rootLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frameLayout.addView(scrollView);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(60, 70, 60, 70);
        cardLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable glassDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#4DFFFFFF"), Color.parseColor("#1AFFFFFF")}
        );
        glassDrawable.setCornerRadius(80f);
        glassDrawable.setStroke(2, Color.parseColor("#66FFFFFF"));
        cardLayout.setBackground(glassDrawable);
        cardLayout.setElevation(30f);

        TextView title = new TextView(this);
        title.setText("NEO GLASS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(36f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        cardLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("解除系统限制 · 满血高刷新率引擎");
        subtitle.setTextColor(Color.parseColor("#E6FFFFFF"));
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 60);
        cardLayout.addView(subtitle);

        // 核心功能开关
        addSwitch(cardLayout, "强制全局最高刷新率", "max_refresh_rate", "打破 App 锁帧，全程满血运行");

        // 一键重启 SystemUI 按钮
        TextView restartBtn = new TextView(this);
        restartBtn.setText("一键重启系统界面 (Soft Reboot)");
        restartBtn.setTextColor(Color.WHITE);
        restartBtn.setTextSize(16f);
        restartBtn.setTypeface(Typeface.DEFAULT_BOLD);
        restartBtn.setGravity(Gravity.CENTER);
        restartBtn.setPadding(40, 40, 40, 40);
        
        GradientDrawable btnDrawable = new GradientDrawable();
        btnDrawable.setColor(Color.parseColor("#3326D0CE")); // 半透明青色
        btnDrawable.setCornerRadius(40f);
        btnDrawable.setStroke(2, Color.parseColor("#8026D0CE"));
        restartBtn.setBackground(btnDrawable);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 40, 0, 20);
        restartBtn.setLayoutParams(btnParams);

        restartBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("重启系统界面")
                .setMessage("即将重启 SystemUI，屏幕会黑屏闪烁一下，用于让模块设置立刻生效。需要授予 Root 权限。")
                .setPositiveButton("确定重启", (dialog, which) -> restartSystemUI())
                .setNegativeButton("取消", null)
                .show();
        });
        cardLayout.addView(restartBtn);

        TextView warn = new TextView(this);
        warn.setText("⚠️ 提示：开启高刷开关后，请务必点击上方按钮重启 SystemUI 以生效。");
        warn.setTextColor(Color.parseColor("#B3FFD700"));
        warn.setTextSize(12f);
        warn.setGravity(Gravity.CENTER);
        warn.setPadding(0, 20, 0, 20);
        cardLayout.addView(warn);

        rootLayout.addView(cardLayout);
        setContentView(frameLayout);
    }

    // 执行 Root 命令重启 SystemUI
    private void restartSystemUI() {
        Toast.makeText(this, "正在重启 SystemUI...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                // pkill 命令在 Android 上通常有效，系统会自动拉起 SystemUI
                os.writeBytes("pkill -f com.android.systemui\n");
                os.writeBytes("exit\n");
                os.flush();
                os.close();
                process.waitFor();
                
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(MainActivity.this, "重启指令已发送", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(MainActivity.this, "Root 权限获取失败或被拒绝", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void startAuroraAnimation() {
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(6000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.setInterpolator(new android.view.animation.LinearInterpolator());

        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int color1 = blendColors(Color.parseColor("#0F2027"), Color.parseColor("#2C5364"), fraction);
            int color2 = blendColors(Color.parseColor("#203A43"), Color.parseColor("#0F2027"), fraction);
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color1, color2});
            backgroundView.setBackground(bg);
        });
        colorAnimator.start();
    }

    private int blendColors(int color1, int color2, float ratio) {
        float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private void addSwitch(LinearLayout parent, String labelText, final String prefKey, String descText) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 20, 0, 20);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        label.setTypeface(Typeface.DEFAULT_BOLD);

        TextView desc = new TextView(this);
        desc.setText(descText);
        desc.setTextColor(Color.parseColor("#80FFFFFF"));
        desc.setTextSize(12f);

        textLayout.addView(label);
        textLayout.addView(desc);

        Switch switchView = new Switch(this);
        boolean isChecked = prefs.getBoolean(prefKey, false);
        switchView.setChecked(isChecked);

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

        row.addView(textLayout);
        row.addView(switchView);
        container.addView(row);
        parent.addView(container);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (colorAnimator != null) colorAnimator.cancel();
    }
}