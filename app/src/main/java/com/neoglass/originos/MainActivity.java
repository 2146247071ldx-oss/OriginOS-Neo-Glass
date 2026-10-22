package com.neoglass.originos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private boolean isActivated = false;
    private XposedService xposedService;

    private int COLOR_BG;
    private int COLOR_CARD;
    private int COLOR_TEXT_MAIN;
    private int COLOR_TEXT_SUB;
    private int COLOR_ACCENT;
    private int COLOR_GREEN;
    private boolean isDarkMode = false;

    private FrameLayout mainRoot;
    private FrameLayout mainTabsContainer;
    private FrameLayout subPageContainer;
    private FrameLayout contentContainer;

    private LinearLayout pageOverview, pageGame, pageSettings;
    private LinearLayout navOverview, navGame, navSettings;
    private TextView tvOverviewIcon, tvOverviewLabel;
    private TextView tvGameIcon, tvGameLabel;
    private TextView tvSettingsIcon, tvSettingsLabel;
    
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);
        applyTheme(); 

        // 核心修复：Android 12+ 原生窗口模糊 (绝对兼容 Android 16)
        if (prefs.getBoolean("theme_blur", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                int blurRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                // 使用官方公开的 API
                getWindow().setBackgroundBlurRadius(blurRadius);
            } catch (Throwable ignored) {}
        }

        try {
            Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml");
        } catch (Exception ignored) {}

        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                xposedService = service;
                isActivated = true;
                runOnUiThread(() -> refreshOverviewStatus());
            }

            @Override
            public void onServiceDied(XposedService service) {
                xposedService = null;
                isActivated = false;
                runOnUiThread(() -> refreshOverviewStatus());
            }
        });

        mainRoot = new FrameLayout(this);
        mainRoot.setBackgroundColor(COLOR_BG);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getDecorView().setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return WindowInsets.CONSUMED;
            });
        }

        initMainTabs();

        subPageContainer = new FrameLayout(this);
        subPageContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        subPageContainer.setBackgroundColor(COLOR_BG);
        subPageContainer.setVisibility(View.GONE);
        subPageContainer.setElevation(dp2px(10));

        mainRoot.addView(mainTabsContainer);
        mainRoot.addView(subPageContainer);

        setContentView(mainRoot);
        switchPage(0, false);
    }

    private void applyTheme() {
        String theme = prefs.getString("theme_mode", "system");
        if ("dark".equals(theme)) {
            isDarkMode = true;
        } else if ("light".equals(theme)) {
            isDarkMode = false;
        } else {
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }

        if (isDarkMode) {
            COLOR_BG = Color.parseColor("#121212");
            COLOR_CARD = Color.parseColor("#1E1E1E");
            COLOR_TEXT_MAIN = Color.parseColor("#F0F0F0");
            COLOR_TEXT_SUB = Color.parseColor("#999999");
            COLOR_GREEN = Color.parseColor("#81C784");
        } else {
            COLOR_BG = Color.parseColor("#F7F8FA");
            COLOR_CARD = Color.parseColor("#FFFFFF");
            COLOR_TEXT_MAIN = Color.parseColor("#111111");
            COLOR_TEXT_SUB = Color.parseColor("#777777");
            COLOR_GREEN = Color.parseColor("#4CAF50");
        }

        if (prefs.getBoolean("theme_monet", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                int colorResId = (isDarkMode) ? android.R.color.system_accent1_400 : android.R.color.system_accent1_200;
                COLOR_ACCENT = getResources().getColor(colorResId, getTheme());
            } catch (Exception e) {
                COLOR_ACCENT = isDarkMode ? Color.parseColor("#66B2FF") : Color.parseColor("#3B82F6");
            }
        } else {
            COLOR_ACCENT = isDarkMode ? Color.parseColor("#66B2FF") : Color.parseColor("#3B82F6");
        }
    }

    private void initMainTabs() {
        mainTabsContainer = new FrameLayout(this);
        mainTabsContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        contentParams.setMargins(0, 0, 0, dp2px(100)); 
        mainTabsContainer.addView(contentContainer, contentParams);

        initOverviewPage();
        initGamePage();
        initSettingsPage();

        boolean isFloating = prefs.getBoolean("theme_floating_bar", true);
        boolean isGlass = prefs.getBoolean("theme_liquid_glass", false);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        if (isFloating) {
            bottomBar.setPadding(dp2px(16), dp2px(12), dp2px(16), dp2px(12));
            
            if (isGlass) {
                GradientDrawable glassBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
                    new int[]{ isDarkMode ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#A6FFFFFF"), 
                               isDarkMode ? Color.parseColor("#1A000000") : Color.parseColor("#40FFFFFF") });
                glassBg.setCornerRadius(dp2px(30));
                glassBg.setStroke(dp2px(1), isDarkMode ? Color.parseColor("#66FFFFFF") : Color.parseColor("#CCFFFFFF"));
                bottomBar.setBackground(glassBg);
                bottomBar.setElevation(dp2px(16));
                bottomBar.setOutlineAmbientShadowColor(Color.BLACK);
                bottomBar.setOutlineSpotShadowColor(Color.BLACK);
            } else {
                GradientDrawable barBg = new GradientDrawable();
                barBg.setCornerRadius(dp2px(30));
                barBg.setColor(isDarkMode ? Color.parseColor("#2A2A2A") : Color.parseColor("#F2F3F5"));
                bottomBar.setBackground(barBg);
                bottomBar.setElevation(dp2px(4));
            }
            barParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            barParams.setMargins(0, 0, 0, dp2px(24));
        } else {
            bottomBar.setPadding(dp2px(16), dp2px(16), dp2px(16), dp2px(16));
            GradientDrawable barBg = new GradientDrawable();
            barBg.setColor(isDarkMode ? Color.parseColor("#1E1E1E") : Color.parseColor("#FFFFFF"));
            barBg.setCornerRadius(0f);
            bottomBar.setBackground(barBg);
            barParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            barParams.gravity = Gravity.BOTTOM;
            bottomBar.setElevation(dp2px(8));
        }

        mainTabsContainer.addView(bottomBar, barParams);

        navOverview = createNavItem("🏠", "概览", bottomBar);
        navGame = createNavItem("🎮", "游戏", bottomBar);
        navSettings = createNavItem("⚙️", "设置", bottomBar);

        tvOverviewIcon = (TextView) navOverview.getChildAt(0);
        tvOverviewLabel = (TextView) navOverview.getChildAt(1);
        tvGameIcon = (TextView) navGame.getChildAt(0);
        tvGameLabel = (TextView) navGame.getChildAt(1);
        tvSettingsIcon = (TextView) navSettings.getChildAt(0);
        tvSettingsLabel = (TextView) navSettings.getChildAt(1);

        navOverview.setOnClickListener(v -> switchPage(0, true));
        navGame.setOnClickListener(v -> switchPage(1, true));
        navSettings.setOnClickListener(v -> switchPage(2, true));
    }

    private void switchPage(int index, boolean animate) {
        currentPage = index;
        subPageContainer.setVisibility(View.GONE);
        
        if (animate) {
            contentContainer.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                contentContainer.removeAllViews();
                updateNavUI(index);
                contentContainer.animate().alpha(1f).setDuration(200).start();
            }).start();
        } else {
            contentContainer.removeAllViews();
            updateNavUI(index);
        }
    }

    private void updateNavUI(int index) {
        tvOverviewIcon.setAlpha(0.4f); tvOverviewLabel.setTextColor(COLOR_TEXT_SUB); navOverview.setBackground(null);
        tvGameIcon.setAlpha(0.4f); tvGameLabel.setTextColor(COLOR_TEXT_SUB); navGame.setBackground(null);
        tvSettingsIcon.setAlpha(0.4f); tvSettingsLabel.setTextColor(COLOR_TEXT_SUB); navSettings.setBackground(null);

        boolean isGlass = prefs.getBoolean("theme_liquid_glass", false);
        int activeBgColor;
        
        if (isGlass) {
            activeBgColor = isDarkMode ? Color.parseColor("#33FFFFFF") : Color.parseColor("#80FFFFFF");
        } else {
            activeBgColor = isDarkMode ? Color.parseColor("#444444") : Color.parseColor("#E5E7EB");
        }

        if (index == 0) {
            contentContainer.addView(pageOverview);
            tvOverviewIcon.setAlpha(1f); tvOverviewLabel.setTextColor(COLOR_TEXT_MAIN); navOverview.setBackground(createRoundRectBg(activeBgColor, dp2px(20)));
        } else if (index == 1) {
            contentContainer.addView(pageGame);
            tvGameIcon.setAlpha(1f); tvGameLabel.setTextColor(COLOR_TEXT_MAIN); navGame.setBackground(createRoundRectBg(activeBgColor, dp2px(20)));
        } else if (index == 2) {
            contentContainer.addView(pageSettings);
            tvSettingsIcon.setAlpha(1f); tvSettingsLabel.setTextColor(COLOR_TEXT_MAIN); navSettings.setBackground(createRoundRectBg(activeBgColor, dp2px(20)));
        }
    }

    private void openSubPage(String title, LinearLayout contentView) {
        subPageContainer.removeAllViews();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp2px(16), dp2px(16), dp2px(16), dp2px(16));
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView btnBack = new TextView(this);
        btnBack.setText("< 返回");
        btnBack.setTextSize(15);
        btnBack.setTextColor(COLOR_TEXT_MAIN);
        btnBack.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btnBack.setPadding(0, 0, 0, dp2px(16));
        btnBack.setOnClickListener(v -> {
            subPageContainer.animate().translationX(subPageContainer.getWidth()).setDuration(200).withEndAction(() -> subPageContainer.setVisibility(View.GONE)).start();
        });
        root.addView(btnBack);

        TextView tvTitle = createTitle(title);
        root.addView(tvTitle);
        root.addView(contentView);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        subPageContainer.addView(scrollView);

        subPageContainer.setVisibility(View.VISIBLE);
        subPageContainer.setTranslationX(dp2px(300));
        subPageContainer.animate().translationX(0f).setDuration(200).start();
    }

    private void initOverviewPage() {
        pageOverview = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageOverview.getChildAt(0)).getChildAt(0);

        TextView mainTitle = new TextView(this);
        mainTitle.setText("OriginBreaker");
        mainTitle.setTextColor(COLOR_TEXT_MAIN);
        mainTitle.setTextSize(32);
        mainTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp2px(16), 0, dp2px(24));
        mainTitle.setLayoutParams(p);
        root.addView(mainTitle);

        FrameLayout statusCardFrame = new FrameLayout(this);
        statusCardFrame.setBackground(createRoundRectBg(isActivated ? (isDarkMode ? Color.parseColor("#1B3320") : Color.parseColor("#E8F5E9")) : (isDarkMode ? Color.parseColor("#2A2A2A") : Color.parseColor("#F2F3F5")), dp2px(20)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(24));
        statusCardFrame.setLayoutParams(params);
        statusCardFrame.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvStatusTitle = new TextView(this);
        tvStatusTitle.setText(isActivated ? "已入局" : "未激活");
        tvStatusTitle.setTextColor(isActivated ? COLOR_GREEN : COLOR_TEXT_MAIN);
        tvStatusTitle.setTextSize(28);
        tvStatusTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        TextView tvStatusSub = new TextView(this);
        tvStatusSub.setText(isActivated ? "已与 LSPosed 连通，设置可以落地。" : "请在 LSPosed 中勾选本应用与系统框架。");
        tvStatusSub.setTextColor(isActivated ? (isDarkMode ? Color.parseColor("#81C784") : Color.parseColor("#388E3C")) : COLOR_TEXT_SUB);
        tvStatusSub.setTextSize(14);
        tvStatusSub.setPadding(0, dp2px(8), 0, dp2px(24));

        TextView tvStatusFooter = new TextView(this);
        tvStatusFooter.setText("此身已在局中");
        tvStatusFooter.setTextColor(isActivated ? (isDarkMode ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32")) : COLOR_TEXT_SUB);
        tvStatusFooter.setTextSize(13);

        textContainer.addView(tvStatusTitle);
        textContainer.addView(tvStatusSub);
        if (isActivated) textContainer.addView(tvStatusFooter);
        statusCardFrame.addView(textContainer);

        if (isActivated) {
            View checkMark = new View(this) {
                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp2px(6));
                    paint.setColor(COLOR_GREEN);
                    paint.setStrokeCap(Paint.Cap.ROUND);

                    float size = Math.min(getWidth(), getHeight()) - dp2px(10);
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;

                    canvas.drawArc(cx - size/2, cy - size/2, cx + size/2, cy + size/2, -90, 280, false, paint);

                    Path path = new Path();
                    path.moveTo(cx - size * 0.2f, cy);
                    path.lineTo(cx - size * 0.05f, cy + size * 0.15f);
                    path.lineTo(cx + size * 0.25f, cy - size * 0.15f);
                    canvas.drawPath(path, paint);
                }
            };
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp2px(80), dp2px(80));
            checkParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            checkParams.setMargins(0, 0, dp2px(10), 0);
            checkMark.setLayoutParams(checkParams);
            statusCardFrame.addView(checkMark);
        }

        root.addView(statusCardFrame);

        LinearLayout infoCard = createListGroup();
        String appVer = "1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVer = pInfo.versionName;
        } catch (Exception ignored) {}

        addInfoItem(infoCard, "系统版本", Build.DISPLAY);
        addInfoItem(infoCard, "应用版本", appVer);
        addInfoItem(infoCard, "Android 版本", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addInfoItem(infoCard, "LSPosed 版本", xposedService != null ? "LSPosed " + xposedService.getFrameworkVersion() : "等待连接...");
        root.addView(infoCard);

        LinearLayout reloadCard = createBigCard("重载全部界面适配", "覆盖已授权的系统桌面、系统界面等", "↻",
                () -> {
                    new AlertDialog.Builder(this)
                            .setTitle("选择重载方式")
                            .setMessage("重启界面测试截屏、动画等更改。\n请勿随意重启系统框架。")
                            .setPositiveButton("重启界面", (dialog, which) -> runRootCommand("pkill -f com.android.systemui"))
                            .setNegativeButton("取消", null)
                            .show();
                });
        root.addView(reloadCard);
    }

    private void refreshOverviewStatus() {
        initOverviewPage();
        if (currentPage == 0 && pageOverview != null) {
            contentContainer.removeAllViews();
            contentContainer.addView(pageOverview);
        }
    }

    private void initGamePage() {
        pageGame = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageGame.getChildAt(0)).getChildAt(0);

        root.addView(createTitle("游戏强化"));

        root.addView(createBigCard("性能与温控", "防降频、满血调度、温控伪装", "⚡",
                () -> openSubPage("性能与温控", buildThermalSettings())));

        root.addView(createBigCard("画面与渲染", "强制屏幕刷新率、解锁高刷", "🎮",
                () -> openSubPage("画面与渲染", buildRenderSettings())));

        root.addView(createBigCard("网络与延迟", "防断流、网络延迟伪装", "📶",
                () -> openSubPage("网络与延迟", buildNetworkSettings())));

        root.addView(createBigCard("游戏辅助", "解除截屏限制、解锁魔盒隐藏功能", "🛡️",
                () -> openSubPage("游戏辅助", buildHelperSettings())));
    }

    private void initSettingsPage() {
        pageSettings = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageSettings.getChildAt(0)).getChildAt(0);

        root.addView(createTitle("设置"));

        LinearLayout group1 = createListGroup();
        addSwitchItem(group1, "调试日志", "在目标进程输出详细模块日志", "debug_log");

        LinearLayout rowTheme = createBaseItem("主题设置", "深浅模式、液态玻璃与视觉风格");
        rowTheme.setOnClickListener(v -> openSubPage("主题设置", buildThemeSettings()));
        group1.addView(rowTheme);
        
        LinearLayout rowAbout = createBaseItem("关于 OriginBreaker", "版本信息、开源引用与作者链接");
        rowAbout.setOnClickListener(v -> openSubPage("关于", buildAboutSettings()));
        group1.addView(rowAbout);
        root.addView(group1);
    }

    private LinearLayout buildThermalSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "游戏温控欺骗", "系统读取假温度，拒绝降频 (需Root)", "thermal_spoof", this::showAdvancedThermalDialog);
        addSwitchItem(group, "CPU 满血调度", "强制大核最高频率，提升游戏帧率 (需Root)", "cpu_boost", () -> {
            if (prefs.getBoolean("cpu_boost", false)) {
                runRootCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor; " +
                               "echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor; " +
                               "echo performance > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor");
                Toast.makeText(this, "已强制锁定大核最高频率", Toast.LENGTH_SHORT).show();
            } else {
                runRootCommand("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor; " +
                               "echo schedutil > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor; " +
                               "echo schedutil > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor");
                Toast.makeText(this, "已恢复系统默认调度", Toast.LENGTH_SHORT).show();
            }
        });
        addSwitchItem(group, "解除温控锁频", "关闭系统底层温度降频保护 (需Root)", "disable_thermal", () -> {
            if (prefs.getBoolean("disable_thermal", false)) {
                runRootCommand("cmd thermalservice override-status 3");
                Toast.makeText(this, "已尝试禁用温控服务", Toast.LENGTH_SHORT).show();
            } else {
                runRootCommand("cmd thermalservice override-status 0");
                Toast.makeText(this, "已恢复温控服务", Toast.LENGTH_SHORT).show();
            }
        });
        return group;
    }

    private void showAdvancedThermalDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp2px(24), dp2px(16), dp2px(24), dp2px(16));

        TextView tvValue = new TextView(this);
        tvValue.setText("当前设定：25.0 °C");
        tvValue.setTextSize(18);
        tvValue.setTextColor(COLOR_TEXT_MAIN);
        tvValue.setGravity(Gravity.CENTER);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(30); 
        int currentTemp = Integer.parseInt(prefs.getString("thermal_temp_val", "25"));
        seekBar.setProgress(currentTemp - 15);
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvValue.setText("当前设定：" + (progress + 15) + ".0 °C");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(tvValue);
        layout.addView(seekBar);

        new AlertDialog.Builder(this)
                .setTitle("自定义伪装温度")
                .setView(layout)
                .setPositiveButton("应用", (dialog, which) -> {
                    String newTemp = String.valueOf(seekBar.getProgress() + 15);
                    prefs.edit().putString("thermal_temp_val", newTemp).apply();
                    runRootCommand("cmd battery set -f temp " + newTemp);
                    Toast.makeText(this, "已强制写入电池温度: " + newTemp + "°C", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private LinearLayout buildRenderSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "强制 144Hz 高刷", "打破系统限制，强行请求 144Hz 刷新率 (需勾选目标应用)", "force_144hz");
        addSwitchItem(group, "强制抗锯齿", "强制开启 GPU MSAA 多重采样抗锯齿 (需Root)", "force_aa", () -> {
            if (prefs.getBoolean("force_aa", false)) {
                runRootCommand("setprop debug.egl.force_msaa 1");
                Toast.makeText(this, "已尝试强制开启 GPU MSAA (重启游戏生效)", Toast.LENGTH_SHORT).show();
            } else {
                runRootCommand("setprop debug.egl.force_msaa 0");
                Toast.makeText(this, "已关闭强制抗锯齿", Toast.LENGTH_SHORT).show();
            }
        });
        return group;
    }

    private LinearLayout buildNetworkSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "防断流优化", "拦截系统的极速休眠网络切断", "net_keep_alive");
        addSwitchItem(group, "强制 IPv4 解析", "避免部分游戏因 IPv6 解析失败导致的高延迟", "force_ipv4");
        addSwitchItem(group, "网络拥塞控制优化", "尝试启用 BBR 算法，降低延迟 (需Root与内核支持)", "network_bbr", () -> {
            if (prefs.getBoolean("network_bbr", false)) {
                runRootCommand("echo bbr > /proc/sys/net/ipv4/tcp_congestion_control");
                Toast.makeText(this, "已尝试将 TCP 拥塞控制设为 BBR", Toast.LENGTH_SHORT).show();
            } else {
                runRootCommand("echo cubic > /proc/sys/net/ipv4/tcp_congestion_control");
                Toast.makeText(this, "已恢复系统默认拥塞控制 (cubic)", Toast.LENGTH_SHORT).show();
            }
        });
        return group;
    }

    private LinearLayout buildHelperSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "解除截屏限制", "允许在游戏中随意截图/录屏", "screen_snap");
        addSwitchItem(group, "解锁游戏魔盒", "强行开启散热风扇、m+模式等隐藏UI", "unlock_gamecube");
        return group;
    }

    private LinearLayout buildThemeSettings() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout modeGroup = new LinearLayout(this);
        modeGroup.setOrientation(LinearLayout.HORIZONTAL);
        modeGroup.setGravity(Gravity.CENTER);
        modeGroup.setPadding(0, 0, 0, dp2px(16));

        String[] modes = {"跟随系统", "浅色", "深色"};
        String[] modeVals = {"system", "light", "dark"};
        String currentMode = prefs.getString("theme_mode", "system");

        for (int i = 0; i < 3; i++) {
            TextView btn = new TextView(this);
            btn.setText(modes[i]);
            btn.setTextSize(14);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, dp2px(12), 0, dp2px(12));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            bp.setMargins(dp2px(4), 0, dp2px(4), 0);
            btn.setLayoutParams(bp);
            
            boolean isSelected = currentMode.equals(modeVals[i]);
            btn.setBackground(createRoundRectBg(isSelected ? COLOR_ACCENT : COLOR_CARD, dp2px(12)));
            btn.setTextColor(isSelected ? Color.WHITE : COLOR_TEXT_MAIN);

            final String val = modeVals[i];
            btn.setOnClickListener(v -> {
                prefs.edit().putString("theme_mode", val).apply();
                recreate(); 
            });
            modeGroup.addView(btn);
        }
        root.addView(modeGroup);

        LinearLayout group = createListGroup();
        addSwitchItem(group, "启用 Monet 颜色", "跟随系统壁纸动态取色 (需 Android 12+)", "theme_monet", this::recreate);
        addSwitchItem(group, "全局模糊效果", "启用系统底层的窗口模糊效果", "theme_blur", this::recreate);
        addSwitchItem(group, "悬浮底栏", "使用 Apple 风格的悬浮底栏", "theme_floating_bar", this::recreate);
        addSwitchItem(group, "液态玻璃", "启用悬浮底栏的液态玻璃反光效果", "theme_liquid_glass", this::recreate);
        root.addView(group);
        return root;
    }

    private LinearLayout buildAboutSettings() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = createListGroup();
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp2px(32), 0, dp2px(32));

        TextView tvAvatar = new TextView(this);
        tvAvatar.setText("🛡️");
        tvAvatar.setTextSize(64);
        tvAvatar.setGravity(Gravity.CENTER);
        header.addView(tvAvatar);

        TextView tvName = new TextView(this);
        tvName.setText("OriginBreaker");
        tvName.setTextSize(24);
        tvName.setTextColor(COLOR_TEXT_MAIN);
        tvName.setTypeface(Typeface.create("serif", Typeface.BOLD));
        tvName.setPadding(0, dp2px(16), 0, dp2px(8));
        header.addView(tvName);

        TextView tvSlogan = new TextView(this);
        tvSlogan.setText("突破限制，释放潜能。");
        tvSlogan.setTextSize(14);
        tvSlogan.setTextColor(COLOR_TEXT_SUB);
        header.addView(tvSlogan);

        TextView tvVersion = new TextView(this);
        tvVersion.setText("版本 v2.0.0-api102");
        tvVersion.setTextSize(13);
        tvVersion.setTextColor(COLOR_TEXT_SUB);
        tvVersion.setPadding(0, dp2px(8), 0, 0);
        header.addView(tvVersion);
        root.addView(header);

        LinearLayout descGroup = createListGroup();
        TextView tvDesc = new TextView(this);
        tvDesc.setText("一款专为 OriginOS 打造的底层性能调度与游戏辅助模块。基于 libxposed 102 API，助你掌控温控、高刷与系统隐藏功能。");
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(COLOR_TEXT_MAIN);
        tvDesc.setLineSpacing(dp2px(4), 1f);
        tvDesc.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(20));
        descGroup.addView(tvDesc);
        root.addView(descGroup);

        TextView tvOpenSource = createSubTitle("开源引用");
        root.addView(tvOpenSource);
        LinearLayout osGroup = createListGroup();
        addInfoItem(osGroup, "libxposed", "102");
        addInfoItem(osGroup, "AndroidX", "最新版本");
        root.addView(osGroup);

        TextView tvLinks = createSubTitle("项目链接");
        root.addView(tvLinks);
        LinearLayout linkGroup = createListGroup();
        
        LinearLayout rowAuthor = createBaseItem("作者酷安主页", "惩煦");
        rowAuthor.setOnClickListener(v -> openCoolapkAuthorPage());
        linkGroup.addView(rowAuthor);
        root.addView(linkGroup);

        TextView tvDisclaimer = createSubTitle("免责声明");
        root.addView(tvDisclaimer);
        LinearLayout disGroup = createListGroup();
        TextView tvDisText = new TextView(this);
        tvDisText.setText("本模块完全免费使用，不存在收费盈利。\n所有功能仅供学习交流与技术研究使用，请勿用于商业或违法用途。\n因使用本模块产生的任何后果，由使用者自行承担。");
        tvDisText.setTextSize(13);
        tvDisText.setTextColor(COLOR_TEXT_SUB);
        tvDisText.setLineSpacing(dp2px(4), 1f);
        tvDisText.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(20));
        disGroup.addView(tvDisText);
        root.addView(disGroup);

        return root;
    }

    private void openCoolapkAuthorPage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("coolmarket://u/39927079"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/39927079"));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private TextView createSubTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(COLOR_ACCENT);
        tv.setPadding(dp2px(8), dp2px(16), 0, dp2px(8));
        return tv;
    }

    private LinearLayout createNavItem(String iconStr, String textStr, LinearLayout parent) {
        LinearLayout navLayout = new LinearLayout(this);
        navLayout.setOrientation(LinearLayout.VERTICAL);
        navLayout.setGravity(Gravity.CENTER);
        navLayout.setPadding(dp2px(24), dp2px(10), dp2px(24), dp2px(10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp2px(4), 0, dp2px(4), 0);
        navLayout.setLayoutParams(p);

        TextView icon = new TextView(this);
        icon.setText(iconStr);
        icon.setTextSize(20);
        icon.setGravity(Gravity.CENTER);

        TextView label = new TextView(this);
        label.setText(textStr);
        label.setTextSize(12);
        label.setPadding(0, dp2px(4), 0, 0);
        label.setGravity(Gravity.CENTER);

        navLayout.addView(icon);
        navLayout.addView(label);
        parent.addView(navLayout);
        return navLayout;
    }

    private LinearLayout createBigCard(String title, String desc, String icon, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(20));
        card.setBackground(createRoundRectBg(COLOR_CARD, dp2px(20)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(16));
        card.setLayoutParams(params);
        card.setElevation(dp2px(2));

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(28);
        tvIcon.setPadding(0, 0, dp2px(16), 0);
        card.addView(tvIcon);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(COLOR_TEXT_MAIN);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(COLOR_TEXT_SUB);
        tvDesc.setTextSize(13);
        tvDesc.setPadding(0, dp2px(4), 0, 0);

        textLayout.addView(tvTitle);
        textLayout.addView(tvDesc);
        card.addView(textLayout);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(COLOR_TEXT_SUB);
        arrow.setTextSize(24);
        card.addView(arrow);

        card.setOnClickListener(v -> onClick.run());
        return card;
    }

    private TextView createTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_TEXT_MAIN);
        title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp2px(24));
        title.setLayoutParams(p);
        return title;
    }

    private LinearLayout createBaseScroll() {
        LinearLayout frame = new LinearLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp2px(16), dp2px(16), dp2px(16), dp2px(16));
        root.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollView.addView(root);
        frame.addView(scrollView);
        return frame;
    }

    private LinearLayout createListGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(createRoundRectBg(COLOR_CARD, dp2px(20)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(24));
        group.setLayoutParams(params);
        return group;
    }

    private LinearLayout createBaseItem(String labelText, String descText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(20));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(COLOR_TEXT_MAIN);
        label.setTextSize(16);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView desc = new TextView(this);
        desc.setText(descText);
        desc.setTextColor(COLOR_TEXT_SUB);
        desc.setTextSize(13);
        desc.setPadding(0, dp2px(6), 0, 0);

        textLayout.addView(label);
        textLayout.addView(desc);
        row.addView(textLayout);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(COLOR_TEXT_SUB);
        arrow.setTextSize(20);
        row.addView(arrow);

        return row;
    }

    private void addSwitchItem(LinearLayout parent, String labelText, String descText, final String prefKey) {
        addSwitchItem(parent, labelText, descText, prefKey, null);
    }

    private void addSwitchItem(LinearLayout parent, String labelText, String descText, final String prefKey, Runnable onChangedCallback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(20));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(COLOR_TEXT_MAIN);
        label.setTextSize(16);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView desc = new TextView(this);
        desc.setText(descText);
        desc.setTextColor(COLOR_TEXT_SUB);
        desc.setTextSize(13);
        desc.setPadding(0, dp2px(6), 0, 0);

        textLayout.addView(label);
        textLayout.addView(desc);
        row.addView(textLayout);

        Switch switchView = new Switch(this);
        boolean defaultValue = prefKey.equals("theme_floating_bar");
        switchView.setChecked(prefs.getBoolean(prefKey, defaultValue));
        
        switchView.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(prefKey, checked).apply();
            if (onChangedCallback != null) {
                onChangedCallback.run();
            } else {
                Toast.makeText(this, labelText + (checked ? " 已开启" : " 已关闭"), Toast.LENGTH_SHORT).show();
            }
        });
        row.addView(switchView);
        parent.addView(row);
    }

    private void addInfoItem(LinearLayout parent, String title, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp2px(24), dp2px(20), dp2px(24), dp2px(20));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(COLOR_TEXT_MAIN);
        t.setTextSize(16);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(COLOR_TEXT_SUB);
        v.setTextSize(14);
        v.setGravity(Gravity.RIGHT);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(t);
        row.addView(v);
        parent.addView(row);
    }

    private GradientDrawable createRoundRectBg(int color, float radiusPx) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusPx);
        return shape;
    }

    private void runRootCommand(String cmd) {
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

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}