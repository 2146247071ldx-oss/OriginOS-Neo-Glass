package com.neoglass.originos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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
    
    // 动态颜色变量
    private int COLOR_BG;
    private int COLOR_CARD;
    private int COLOR_TEXT_MAIN;
    private int COLOR_TEXT_SUB;
    private int COLOR_ACCENT;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);
        try {
            Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml");
        } catch (Exception ignored) {}

        // 初始化颜色（读取系统深浅色与 Monet 状态）
        initColors();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.setFillViewport(true);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(60, 100, 60, 100);
        scrollView.addView(rootLayout);

        // ================= 顶部大标题 =================
        TextView title = new TextView(this);
        title.setText("Origin Engine");
        title.setTextColor(COLOR_TEXT_MAIN);
        title.setTextSize(36f);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        title.setPadding(0, 0, 0, 40);
        rootLayout.addView(title);

        // ================= 卡片 0：主题设置 =================
        LinearLayout themeCard = createCard();
        addCategoryTitle(themeCard, "🎨 主题设置");
        
        // 1. 深色模式选择（模拟 3 个按钮）
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setPadding(0, 10, 0, 30);
        
        int currentMode = prefs.getInt("theme_mode", 0); // 0跟随, 1浅色, 2深色
        String[] modes = {"跟随系统", "浅色", "深色"};
        for (int i = 0; i < 3; i++) {
            TextView btn = new TextView(this);
            btn.setText(modes[i]);
            btn.setTextSize(14f);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, 20, 0, 20);
            
            // 按钮背景
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(30f);
            if (currentMode == i) {
                bg.setColor(COLOR_ACCENT); // 选中状态用强调色
                btn.setTextColor(Color.WHITE);
            } else {
                bg.setColor(Color.parseColor("#15000000")); // 未选中半透明
                btn.setTextColor(COLOR_TEXT_MAIN);
            }
            btn.setBackground(bg);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p.setMargins(10, 0, 10, 0);
            btn.setLayoutParams(p);
            
            final int modeIndex = i;
            btn.setOnClickListener(v -> {
                prefs.edit().putInt("theme_mode", modeIndex).apply();
                recreate(); // 重启 Activity 应用新主题
            });
            modeRow.addView(btn);
        }
        themeCard.addView(modeRow);

        // 2. 启用 Monet 颜色开关
        addSwitchRow(themeCard, "启用 Monet 颜色", "monet_enabled", "提取壁纸颜色，自动适配全局 UI", true);

        rootLayout.addView(themeCard);

        // ================= 卡片 1：系统美化 =================
        LinearLayout beautyCard = createCard();
        addCategoryTitle(beautyCard, "✨ 系统美化");
        addSwitchRow(beautyCard, "全局 Q 弹动画", "spring_anim", "替换底层插值器，带来果冻般跟手感", false);
        addSwitchRow(beautyCard, "通知卡片极光化", "glass_notification", "强行修改通知面板背景和圆角，模拟毛玻璃", false);
        rootLayout.addView(beautyCard);

        // ================= 卡片 2：电池保护 =================
        LinearLayout batteryCard = createCard();
        addCategoryTitle(batteryCard, "🔋 电池保护");
        addSwitchRow(batteryCard, "限制充电上限 (80%)", "limit_charge", "达到80%系统视为已满，减缓电池老化", false);
        addSwitchRow(batteryCard, "息屏断网休眠 (Wakelock杀手)", "wakelock_killer", "息屏后强制没收后台流氓唤醒锁，极速省电", false);
        rootLayout.addView(batteryCard);

        // ================= 卡片 3：重启按钮 =================
        LinearLayout actionCard = createCard();
        actionCard.setOrientation(LinearLayout.HORIZONTAL);
        actionCard.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView icon = new TextView(this);
        icon.setText("↻");
        icon.setTextColor(COLOR_ACCENT);
        icon.setTextSize(30f);
        icon.setPadding(0, 0, 30, 0);
        actionCard.addView(icon);

        LinearLayout actionTextLayout = new LinearLayout(this);
        actionTextLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView actionTitle = new TextView(this);
        actionTitle.setText("重启界面 / 框架 (双模)");
        actionTitle.setTextColor(COLOR_TEXT_MAIN);
        actionTitle.setTextSize(16f);
        actionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        
        TextView actionDesc = new TextView(this);
        actionDesc.setText("重载系统 UI 或底层框架，使设置生效");
        actionDesc.setTextColor(COLOR_TEXT_SUB);
        actionDesc.setTextSize(12f);
        actionDesc.setPadding(0, 5, 0, 0);

        actionTextLayout.addView(actionTitle);
        actionTextLayout.addView(actionDesc);
        actionCard.addView(actionTextLayout);

        actionCard.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("选择重启方式")
                .setMessage("【系统界面】：黑屏1秒，测试美化（跑马灯/动画）。\n【系统框架】：黑屏5秒，测试底层（电池/唤醒）。")
                .setPositiveButton("仅重启系统界面", (dialog, which) -> runRootCommand("pkill -f com.android.systemui"))
                .setNeutralButton("重启系统框架", (dialog, which) -> runRootCommand("killall system_server"))
                .setNegativeButton("取消", null)
                .show();
        });
        rootLayout.addView(actionCard);

        // 底部
        TextView author = new TextView(this);
        author.setText("酷安: @惩煦");
        author.setTextColor(COLOR_TEXT_SUB);
        author.setTextSize(12f);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 50, 0, 0);
        author.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.coolapk.com/u/39927079")));
            } catch (Exception ignored) {}
        });
        rootLayout.addView(author);

        setContentView(scrollView);
    }

    // 动态初始化颜色
    private void initColors() {
        int mode = prefs.getInt("theme_mode", 0);
        boolean monet = prefs.getBoolean("monet_enabled", false);
        boolean isSystemDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        
        boolean useDark = false;
        if (mode == 0) useDark = isSystemDark;
        else if (mode == 2) useDark = true;

        // 1. 如果有 Monet 且系统 >= Android 12 (API 31)
        if (monet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // 强行读取系统原生的 Monet 取色资源
                COLOR_ACCENT = getResources().getColor(useDark ? android.R.color.system_accent1_200 : android.R.color.system_accent1_500, getTheme());
                COLOR_BG = getResources().getColor(useDark ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_50, getTheme());
                COLOR_CARD = getResources().getColor(useDark ? android.R.color.system_neutral1_800 : android.R.color.system_neutral1_100, getTheme());
                COLOR_TEXT_MAIN = getResources().getColor(useDark ? android.R.color.system_neutral1_100 : android.R.color.system_neutral1_900, getTheme());
                COLOR_TEXT_SUB = getResources().getColor(useDark ? android.R.color.system_neutral1_300 : android.R.color.system_neutral1_700, getTheme());
                return;
            } catch (Throwable ignored) {}
        }

        // 2. 降级方案（未开启 Monet 或系统版本过低）
        if (useDark) {
            COLOR_BG = Color.parseColor("#1A1A1A");
            COLOR_CARD = Color.parseColor("#282828");
            COLOR_TEXT_MAIN = Color.parseColor("#F0F0F0");
            COLOR_TEXT_SUB = Color.parseColor("#888888");
            COLOR_ACCENT = Color.parseColor("#8C9EFF"); // 淡蓝色
        } else {
            COLOR_BG = Color.parseColor("#F7F8FA");
            COLOR_CARD = Color.parseColor("#FFFFFF");
            COLOR_TEXT_MAIN = Color.parseColor("#1A1A1A");
            COLOR_TEXT_SUB = Color.parseColor("#888888");
            COLOR_ACCENT = Color.parseColor("#5C6BC0"); // 淡紫蓝色
        }
    }

    // ================= 辅助方法 =================

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(50, 50, 50, 50);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(COLOR_CARD);
        shape.setCornerRadius(60f);
        card.setBackground(shape);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 40);
        card.setLayoutParams(params);
        card.setElevation(10f);
        
        return card;
    }

    private void addCategoryTitle(LinearLayout parent, String titleText) {
        TextView cat = new TextView(this);
        cat.setText(titleText);
        cat.setTextColor(COLOR_TEXT_MAIN);
        cat.setTextSize(18f);
        cat.setTypeface(Typeface.DEFAULT_BOLD);
        cat.setPadding(0, 0, 0, 30);
        parent.addView(cat);
    }

    private void addSwitchRow(LinearLayout parent, String labelText, final String prefKey, String descText, boolean forceRecreate) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 20, 0, 20);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(COLOR_TEXT_MAIN);
        label.setTextSize(16f);

        TextView desc = new TextView(this);
        desc.setText(descText);
        desc.setTextColor(COLOR_TEXT_SUB);
        desc.setTextSize(12f);
        desc.setPadding(0, 5, 0, 0);

        textLayout.addView(label);
        textLayout.addView(desc);

        Switch switchView = new Switch(this);
        switchView.setChecked(prefs.getBoolean(prefKey, false));
        
        // 强制把开关的颜色替换为 Monet 的强调色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.content.res.ColorStateList thumbStateList = new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{COLOR_ACCENT, Color.parseColor("#AAAAAA")}
            );
            android.content.res.ColorStateList trackStateList = new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.argb(100, Color.red(COLOR_ACCENT), Color.green(COLOR_ACCENT), Color.blue(COLOR_ACCENT)), Color.parseColor("#40000000")}
            );
            try {
                switchView.setThumbTintList(thumbStateList);
                switchView.setTrackTintList(trackStateList);
            } catch (Throwable ignored) {}
        }

        switchView.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(prefKey, checked).apply();
            try { Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml"); } catch (Exception ignored) {}
            
            // 如果修改的是 Monet 开关，立刻重启界面让颜色生效
            if (forceRecreate) {
                recreate();
            }
        });

        row.addView(textLayout);
        row.addView(switchView);
        parent.addView(row);
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
}