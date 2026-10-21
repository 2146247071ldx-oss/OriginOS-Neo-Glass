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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
        
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);
        try {
            Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml");
        } catch (Exception ignored) {}

        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);

        backgroundView = new View(this);
        frameLayout.addView(backgroundView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        startAuroraAnimation();

        ScrollView scrollView = new ScrollView(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(40, 80, 40, 80);
        rootLayout.setGravity(Gravity.CENTER);
        scrollView.addView(rootLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frameLayout.addView(scrollView);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(50, 60, 50, 60);
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
        title.setText("OriginOS 极客引擎");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        cardLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("全面美化 · 底层控制");
        subtitle.setTextColor(Color.parseColor("#E6FFFFFF"));
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 40);
        cardLayout.addView(subtitle);

        // === 美化分类 ===
        addCategoryTitle(cardLayout, "✨ 系统美化");
        addSwitch(cardLayout, "全局 Q 弹动画", "spring_anim", "替换底层插值器，带来果冻般跟手感");
        addSwitch(cardLayout, "通知卡片极光化", "glass_notification", "强行修改通知面板背景和圆角，模拟毛玻璃");

        // === 电池分类 ===
        addCategoryTitle(cardLayout, "🔋 电池保护");
        addSwitch(cardLayout, "限制充电上限 (80%)", "limit_charge", "达到80%系统视为已满，减缓电池老化");
        addSwitch(cardLayout, "息屏断网休眠 (Wakelock杀手)", "wakelock_killer", "息屏后强制没收后台流氓唤醒锁，极速省电");

        // 一键崩溃重启系统界面按钮（UI美化测试用）
        TextView restartBtn = new TextView(this);
        restartBtn.setText("重启界面 / 框架 (双模)");
        restartBtn.setTextColor(Color.WHITE);
        restartBtn.setTextSize(15f);
        restartBtn.setTypeface(Typeface.DEFAULT_BOLD);
        restartBtn.setGravity(Gravity.CENTER);
        restartBtn.setPadding(40, 40, 40, 40);
        
        GradientDrawable btnDrawable = new GradientDrawable();
        btnDrawable.setColor(Color.parseColor("#33FF9800")); 
        btnDrawable.setCornerRadius(40f);
        btnDrawable.setStroke(2, Color.parseColor("#80FF9800"));
        restartBtn.setBackground(btnDrawable);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 40, 0, 20);
        restartBtn.setLayoutParams(btnParams);

        restartBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("选择重启方式")
                .setMessage("【系统界面】：黑屏1秒，测试美化（跑马灯/动画）。\n【系统框架】：黑屏5秒，测试底层（电池/唤醒）。")
                .setPositiveButton("仅重启系统界面", (dialog, which) -> runRootCommand("pkill -f com.android.systemui"))
                .setNeutralButton("重启系统框架", (dialog, which) -> runRootCommand("killall system_server"))
                .setNegativeButton("取消", null)
                .show();
        });
        cardLayout.addView(restartBtn);

        TextView author = new TextView(this);
        author.setText("酷安: @惩煦"); 
        author.setTextColor(Color.parseColor("#80FFFFFF")); 
        author.setTextSize(13f);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 30, 0, 10);
        author.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.coolapk.com/u/39927079")));
            } catch (Exception ignored) {}
        });
        cardLayout.addView(author);

        rootLayout.addView(cardLayout);
        setContentView(frameLayout);
    }

    private void runRootCommand(String cmd) {
        Toast.makeText(this, "正在执行...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(cmd + "\n"); 
                os.writeBytes("exit\n");
                os.flush();
                os.close();
                process.waitFor();
            } catch (Exception ignored) {}
        }).start();
    }

    private void addCategoryTitle(LinearLayout parent, String titleText) {
        TextView cat = new TextView(this);
        cat.setText(titleText);
        cat.setTextColor(Color.parseColor("#26D0CE"));
        cat.setTextSize(15f);
        cat.setTypeface(Typeface.DEFAULT_BOLD);
        cat.setPadding(0, 30, 0, 10);
        parent.addView(cat);
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
        container.setPadding(0, 15, 0, 15);

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
        desc.setPadding(0, 5, 0, 0);

        textLayout.addView(label);
        textLayout.addView(desc);

        Switch switchView = new Switch(this);
        switchView.setChecked(prefs.getBoolean(prefKey, false));
        
        try {
            switchView.setThumbTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.parseColor("#26D0CE"), Color.parseColor("#E6FFFFFF")}));
            switchView.setTrackTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.parseColor("#4DFFFFFF"), Color.parseColor("#40FFFFFF")}));
        } catch (Throwable ignored) {}

        switchView.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(prefKey, checked).apply();
            try { Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml"); } catch (Exception ignored) {}
        });

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