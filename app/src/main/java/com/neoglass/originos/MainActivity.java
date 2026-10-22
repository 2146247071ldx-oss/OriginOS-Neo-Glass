package com.neoglass.originos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    private int COLOR_BG = Color.parseColor("#F7F8FA");
    private int COLOR_CARD = Color.parseColor("#FFFFFF");
    private int COLOR_TEXT_MAIN = Color.parseColor("#111111");
    private int COLOR_TEXT_SUB = Color.parseColor("#777777");
    private int COLOR_ACCENT = Color.parseColor("#3B82F6");
    private int COLOR_GREEN = Color.parseColor("#4CAF50");

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
        switchPage(0);
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

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp2px(16), dp2px(12), dp2px(16), dp2px(12));

        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.parseColor("#F2F3F5"));
        barBg.setCornerRadius(dp2px(30));
        bottomBar.setBackground(barBg);
        bottomBar.setElevation(dp2px(4));

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        barParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        barParams.setMargins(0, 0, 0, dp2px(24));
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

        navOverview.setOnClickListener(v -> switchPage(0));
        navGame.setOnClickListener(v -> switchPage(1));
        navSettings.setOnClickListener(v -> switchPage(2));
    }

    private void switchPage(int index) {
        currentPage = index;
        subPageContainer.setVisibility(View.GONE);
        contentContainer.removeAllViews();

        tvOverviewIcon.setAlpha(0.4f); tvOverviewLabel.setTextColor(COLOR_TEXT_SUB); navOverview.setBackground(null);
        tvGameIcon.setAlpha(0.4f); tvGameLabel.setTextColor(COLOR_TEXT_SUB); navGame.setBackground(null);
        tvSettingsIcon.setAlpha(0.4f); tvSettingsLabel.setTextColor(COLOR_TEXT_SUB); navSettings.setBackground(null);

        if (index == 0) {
            contentContainer.addView(pageOverview);
            tvOverviewIcon.setAlpha(1f); tvOverviewLabel.setTextColor(COLOR_TEXT_MAIN); navOverview.setBackground(createRoundRectBg(Color.parseColor("#E5E7EB"), dp2px(20)));
        } else if (index == 1) {
            contentContainer.addView(pageGame);
            tvGameIcon.setAlpha(1f); tvGameLabel.setTextColor(COLOR_TEXT_MAIN); navGame.setBackground(createRoundRectBg(Color.parseColor("#E5E7EB"), dp2px(20)));
        } else if (index == 2) {
            contentContainer.addView(pageSettings);
            tvSettingsIcon.setAlpha(1f); tvSettingsLabel.setTextColor(COLOR_TEXT_MAIN); navSettings.setBackground(createRoundRectBg(Color.parseColor("#E5E7EB"), dp2px(20)));
        }
    }

    private void openSubPage(String title, LinearLayout contentView) {
        subPageContainer.removeAllViews();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));

        TextView btnBack = new TextView(this);
        btnBack.setText("< 返回");
        btnBack.setTextSize(14);
        btnBack.setTextColor(COLOR_ACCENT);
        btnBack.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btnBack.setPadding(0, 0, 0, dp2px(16));
        btnBack.setOnClickListener(v -> subPageContainer.setVisibility(View.GONE));
        root.addView(btnBack);

        TextView tvTitle = createTitle(title);
        root.addView(tvTitle);
        root.addView(contentView);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        subPageContainer.addView(scrollView);

        subPageContainer.setVisibility(View.VISIBLE);
        subPageContainer.setAlpha(0f);
        subPageContainer.animate().alpha(1f).setDuration(200).start();
    }

    private void initOverviewPage() {
        pageOverview = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageOverview.getChildAt(0)).getChildAt(0);

        TextView mainTitle = new TextView(this);
        mainTitle.setText("OriginBreaker");
        mainTitle.setTextColor(COLOR_TEXT_MAIN);
        mainTitle.setTextSize(36);
        mainTitle.setTypeface(Typeface.create("serif", Typeface.BOLD));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp2px(16), 0, dp2px(24));
        mainTitle.setLayoutParams(p);
        root.addView(mainTitle);

        FrameLayout statusCardFrame = new FrameLayout(this);
        statusCardFrame.setBackground(createRoundRectBg(isActivated ? Color.parseColor("#E8F5E9") : Color.parseColor("#F2F3F5"), dp2px(20)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(24));
        statusCardFrame.setLayoutParams(params);
        statusCardFrame.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvStatusTitle = new TextView(this);
        tvStatusTitle.setText(isActivated ? "已入局" : "未激活");
        tvStatusTitle.setTextColor(isActivated ? Color.parseColor("#2E7D32") : Color.parseColor("#333333"));
        tvStatusTitle.setTextSize(28);
        tvStatusTitle.setTypeface(Typeface.create("serif", Typeface.BOLD));

        TextView tvStatusSub = new TextView(this);
        tvStatusSub.setText(isActivated ? "已与 LSPosed 连通，设置可以落地。" : "请在 LSPosed 中勾选本应用与系统框架。");
        tvStatusSub.setTextColor(isActivated ? Color.parseColor("#388E3C") : Color.parseColor("#888888"));
        tvStatusSub.setTextSize(14);
        tvStatusSub.setPadding(0, dp2px(8), 0, dp2px(24));

        TextView tvStatusFooter = new TextView(this);
        tvStatusFooter.setText("此身已在局中");
        tvStatusFooter.setTextColor(isActivated ? Color.parseColor("#2E7D32") : Color.parseColor("#888888"));
        tvStatusFooter.setTextSize(13);

        textContainer.addView(tvStatusTitle);
        textContainer.addView(tvStatusSub);
        if (isActivated) textContainer.addView(tvStatusFooter);
        statusCardFrame.addView(textContainer);

        if (isActivated) {
            TextView tvCheck = new TextView(this);
            tvCheck.setText("✓");
            tvCheck.setTextColor(COLOR_GREEN);
            tvCheck.setTextSize(100);
            tvCheck.setTypeface(Typeface.DEFAULT_BOLD);
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            checkParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            checkParams.setMargins(0, 0, dp2px(10), 0);
            tvCheck.setLayoutParams(checkParams);
            statusCardFrame.addView(tvCheck);
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

        LinearLayout rowAbout = createBaseItem("关于", "OriginBreaker 游戏专属模块 v2.0.0");
        rowAbout.setOnClickListener(v -> Toast.makeText(this, "OriginBreaker 游戏专属模块", Toast.LENGTH_SHORT).show());
        group1.addView(rowAbout);
        root.addView(group1);
    }

    private LinearLayout buildThermalSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "游戏温控欺骗", "系统读取假温度，拒绝降频 (需Root)", "thermal_spoof");
        addInputItem(group, "伪装温控数值", "建议 25.0 (摄氏度)，直接写入电池", "thermal_temp", "25.0");
        return group;
    }

    private LinearLayout buildRenderSettings() {
        LinearLayout group = createListGroup();
        // 使用输入框强制设置刷新率
        addInputItem(group, "强制刷新率(Hz)", "输入144解锁高刷，填0恢复默认", "force_refresh_rate", "0");
        return group;
    }

    private LinearLayout buildNetworkSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "防断流优化", "拦截系统的极速休眠网络切断", "net_keep_alive");
        return group;
    }

    private LinearLayout buildHelperSettings() {
        LinearLayout group = createListGroup();
        addSwitchItem(group, "解除截屏限制", "允许在游戏中随意截图/录屏", "screen_snap");
        addSwitchItem(group, "解锁游戏魔盒", "强行开启散热风扇、m+模式等隐藏UI", "unlock_gamecube");
        return group;
    }

    private LinearLayout createNavItem(String iconStr, String textStr, LinearLayout parent) {
        LinearLayout navLayout = new LinearLayout(this);
        navLayout.setOrientation(LinearLayout.VERTICAL);
        navLayout.setGravity(Gravity.CENTER);
        navLayout.setPadding(dp2px(20), dp2px(10), dp2px(20), dp2px(10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
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
        card.setPadding(dp2px(24), dp2px(20), dp2px(24), dp2px(20));
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
        arrow.setTextColor(Color.parseColor("#CCCCCC"));
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
        title.setTypeface(Typeface.create("serif", Typeface.BOLD));
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));

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
        row.setPadding(dp2px(24), dp2px(20), dp2px(24), dp2px(20));
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
        arrow.setTextColor(Color.parseColor("#CCCCCC"));
        arrow.setTextSize(20);
        row.addView(arrow);

        return row;
    }

    private void addSwitchItem(LinearLayout parent, String labelText, String descText, final String prefKey) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp2px(24), dp2px(20), dp2px(24), dp2px(20));
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
        switchView.setChecked(prefs.getBoolean(prefKey, false));
        switchView.setOnCheckedChangeListener((buttonView, checked) -> prefs.edit().putBoolean(prefKey, checked).apply());
        row.addView(switchView);
        parent.addView(row);
    }

    private void addInputItem(LinearLayout parent, String labelText, String descText, final String prefKey, final String defaultValue) {
        LinearLayout row = createBaseItem(labelText, descText);
        row.removeViewAt(row.getChildCount() - 1);

        TextView valueDisplay = new TextView(this);
        valueDisplay.setTextColor(COLOR_TEXT_SUB);
        valueDisplay.setTextSize(15);
        String val = prefs.getString(prefKey, defaultValue);
        valueDisplay.setText(val + " ›");
        row.addView(valueDisplay);

        row.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(labelText);
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setText(val);
            builder.setView(input);
            builder.setPositiveButton("保存", (dialog, which) -> {
                String newValue = input.getText().toString().trim();
                if (!newValue.isEmpty()) {
                    prefs.edit().putString(prefKey, newValue).apply();
                    valueDisplay.setText(newValue + " ›");
                    
                    if ("thermal_temp".equals(prefKey)) {
                        runRootCommand("cmd battery set -f temp " + newValue);
                        Toast.makeText(this, "已修改系统电池温度", Toast.LENGTH_SHORT).show();
                    } else if ("force_refresh_rate".equals(prefKey)) {
                        if ("0".equals(newValue)) {
                            runRootCommand("settings put system min_refresh_rate 60");
                            runRootCommand("settings put system peak_refresh_rate 120");
                            Toast.makeText(this, "已恢复默认刷新率", Toast.LENGTH_SHORT).show();
                        } else {
                            runRootCommand("settings put system min_refresh_rate " + newValue);
                            runRootCommand("settings put system peak_refresh_rate " + newValue);
                            Toast.makeText(this, "已强制锁定屏幕刷新率为 " + newValue + "Hz", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        });
        parent.addView(row);
    }

    private void addInfoItem(LinearLayout parent, String title, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp2px(24), dp2px(16), dp2px(24), dp2px(16));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(COLOR_TEXT_MAIN);
        t.setTextSize(16);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(COLOR_TEXT_SUB);
        v.setTextSize(14);
        v.setPadding(0, dp2px(8), 0, 0);

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