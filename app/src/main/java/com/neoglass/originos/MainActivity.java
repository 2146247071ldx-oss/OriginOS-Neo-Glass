package com.neoglass.originos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

    private int COLOR_BG = Color.parseColor("#F7F8FA"); // 保持轻微浅灰背景
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
    private TextView tvOverviewLabel, tvGameLabel, tvSettingsLabel;
    
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
        contentParams.setMargins(0, 0, 0, dp2px(70));
        mainTabsContainer.addView(contentContainer, contentParams);

        initOverviewPage();
        initGamePage();
        initSettingsPage();

        // 底部导航栏（仿官方贴底设计）
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setBackgroundColor(Color.WHITE);
        bottomBar.setElevation(dp2px(8));
        bottomBar.setPadding(dp2px(16), dp2px(12), dp2px(16), dp2px(12));

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        barParams.gravity = Gravity.BOTTOM;
        mainTabsContainer.addView(bottomBar, barParams);

        navOverview = createNavItem("概览", bottomBar);
        navGame = createNavItem("游戏", bottomBar);
        navSettings = createNavItem("设置", bottomBar);

        tvOverviewLabel = (TextView) navOverview.getChildAt(0);
        tvGameLabel = (TextView) navGame.getChildAt(0);
        tvSettingsLabel = (TextView) navSettings.getChildAt(0);

        navOverview.setOnClickListener(v -> switchPage(0));
        navGame.setOnClickListener(v -> switchPage(1));
        navSettings.setOnClickListener(v -> switchPage(2));
    }

    private void switchPage(int index) {
        currentPage = index;
        subPageContainer.setVisibility(View.GONE);
        contentContainer.removeAllViews();

        // 极简风格：选中变黑，未选中变灰
        tvOverviewLabel.setTextColor(COLOR_TEXT_SUB);
        tvGameLabel.setTextColor(COLOR_TEXT_SUB);
        tvSettingsLabel.setTextColor(COLOR_TEXT_SUB);

        if (index == 0) {
            contentContainer.addView(pageOverview);
            tvOverviewLabel.setTextColor(COLOR_TEXT_MAIN);
        } else if (index == 1) {
            contentContainer.addView(pageGame);
            tvGameLabel.setTextColor(COLOR_TEXT_MAIN);
        } else if (index == 2) {
            contentContainer.addView(pageSettings);
            tvSettingsLabel.setTextColor(COLOR_TEXT_MAIN);
        }
    }

    private void openSubPage(String title, LinearLayout contentView) {
        subPageContainer.removeAllViews();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));

        TextView btnBack = new TextView(this);
        btnBack.setText("< 返回");
        btnBack.setTextSize(15);
        btnBack.setTextColor(COLOR_TEXT_MAIN);
        btnBack.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btnBack.setPadding(0, 0, 0, dp2px(24));
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

        // 顶部标题：居中，细体字
        TextView mainTitle = new TextView(this);
        mainTitle.setText("OriginBreaker");
        mainTitle.setTextColor(COLOR_TEXT_MAIN);
        mainTitle.setTextSize(24);
        mainTitle.setGravity(Gravity.CENTER);
        mainTitle.setTypeface(Typeface.create("serif", Typeface.NORMAL)); // 纤细的衬线体，模仿LSPosed
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp2px(24), 0, dp2px(32));
        mainTitle.setLayoutParams(p);
        root.addView(mainTitle);

        // 状态卡片
        FrameLayout statusCardFrame = new FrameLayout(this);
        statusCardFrame.setBackground(createRoundRectBg(isActivated ? Color.parseColor("#EDF7ED") : Color.parseColor("#F2F3F5"), dp2px(24)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(16));
        statusCardFrame.setLayoutParams(params);
        statusCardFrame.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvStatusTitle = new TextView(this);
        tvStatusTitle.setText(isActivated ? "已激活" : "未激活");
        tvStatusTitle.setTextColor(isActivated ? Color.parseColor("#1B5E20") : Color.parseColor("#333333"));
        tvStatusTitle.setTextSize(22);
        tvStatusTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        TextView tvStatusVer = new TextView(this);
        tvStatusVer.setText(xposedService != null ? "LSPosed " + xposedService.getFrameworkVersion() : "等待连接");
        tvStatusVer.setTextColor(isActivated ? Color.parseColor("#1B5E20") : Color.parseColor("#888888"));
        tvStatusVer.setTextSize(14);
        tvStatusVer.setPadding(0, dp2px(4), 0, 0);

        TextView tvStatusFooter = new TextView(this);
        tvStatusFooter.setText("API 102");
        tvStatusFooter.setTextColor(isActivated ? Color.parseColor("#1B5E20") : Color.parseColor("#888888"));
        tvStatusFooter.setTextSize(14);
        tvStatusFooter.setPadding(0, dp2px(24), 0, 0);

        textContainer.addView(tvStatusTitle);
        textContainer.addView(tvStatusVer);
        textContainer.addView(tvStatusFooter);
        statusCardFrame.addView(textContainer);

        if (isActivated) {
            // 用代码画一个 LSPosed 风格的圆环对勾
            View checkMark = new View(this) {
                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp2px(6));
                    paint.setColor(Color.parseColor("#A5D6A7")); // 浅绿圆环
                    paint.setStrokeCap(Paint.Cap.ROUND);

                    float size = Math.min(getWidth(), getHeight()) - dp2px(10);
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;

                    // 画大半圆
                    canvas.drawArc(cx - size/2, cy - size/2, cx + size/2, cy + size/2, -90, 280, false, paint);

                    // 画对勾
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

        // 信息卡片
        LinearLayout infoCard = createListGroup();
        String appVer = "1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVer = pInfo.versionName;
        } catch (Exception ignored) {}

        addInfoItem(infoCard, "系统版本", Build.DISPLAY);
        addInfoItem(infoCard, "应用版本", appVer);
        addInfoItem(infoCard, "Android 版本", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addInfoItem(infoCard, "设备", Build.MODEL);
        root.addView(infoCard);
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

    // --- 组件构建工具 ---

    private LinearLayout createNavItem(String textStr, LinearLayout parent) {
        LinearLayout navLayout = new LinearLayout(this);
        navLayout.setOrientation(LinearLayout.VERTICAL);
        navLayout.setGravity(Gravity.CENTER);
        navLayout.setPadding(dp2px(16), dp2px(8), dp2px(16), dp2px(8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        navLayout.setLayoutParams(p);

        TextView label = new TextView(this);
        label.setText(textStr);
        label.setTextSize(14);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        navLayout.addView(label);
        parent.addView(navLayout);
        return navLayout;
    }

    private LinearLayout createBigCard(String title, String desc, String icon, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp2px(24), dp2px(24), dp2px(24), dp2px(24));
        card.setBackground(createRoundRectBg(COLOR_CARD, dp2px(20)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(16));
        card.setLayoutParams(params);
        card.setElevation(dp2px(2));

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(24);
        tvIcon.setPadding(0, 0, dp2px(16), 0);
        card.addView(tvIcon);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(COLOR_TEXT_MAIN);
        tvTitle.setTextSize(16);
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
        arrow.setTextSize(22);
        card.addView(arrow);

        card.setOnClickListener(v -> onClick.run());
        return card;
    }

    private TextView createTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_TEXT_MAIN);
        title.setTextSize(24);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp2px(16), 0, dp2px(24));
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
        root.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(20));

        scrollView.addView(root);
        frame.addView(scrollView);
        return frame;
    }

    private LinearLayout createListGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(createRoundRectBg(COLOR_CARD, dp2px(20)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp2px(20));
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
        desc.setPadding(0, dp2px(4), 0, 0);

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
        desc.setPadding(0, dp2px(4), 0, 0);

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
                        Toast.makeText(this, "已修改系统电池温度 (需Root)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "保存成功，重启目标应用生效", Toast.LENGTH_SHORT).show();
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
        row.setPadding(dp2px(24), dp2px(20), dp2px(24), dp2px(20));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(COLOR_TEXT_MAIN);
        t.setTextSize(16);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(COLOR_TEXT_SUB);
        v.setTextSize(14);
        v.setPadding(0, dp2px(6), 0, 0);

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