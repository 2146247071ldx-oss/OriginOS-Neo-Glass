package com.neoglass.originos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
    private int COLOR_BOTTOM_BAR = Color.parseColor("#FFFFFF");

    private FrameLayout contentContainer;
    private LinearLayout pageOverview, pageModule, pageSettings;
    private TextView navOverview, navModule, navSettings;

    private LinearLayout statusCardView;
    private TextView tvStatusTitle;
    private TextView tvStatusSub;
    private TextView tvCheckIcon;
    private TextView tvLsposedVer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);
        try {
            Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml");
        } catch (Exception ignored) {}

        // 注册服务监听，真实查询 LSPosed 激活状态
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                xposedService = service;
                runOnUiThread(() -> updateStatusUI(true));
            }

            @Override
            public void onServiceDied(XposedService service) {
                xposedService = null;
                runOnUiThread(() -> updateStatusUI(false));
            }
        });

        FrameLayout mainRoot = new FrameLayout(this);
        mainRoot.setBackgroundColor(COLOR_BG);

        contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        contentParams.setMargins(0, 0, 0, 200);
        mainRoot.addView(contentContainer, contentParams);

        initOverviewPage();
        initModulePage();
        initSettingsPage();

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(40, 20, 40, 20);

        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(COLOR_BOTTOM_BAR);
        barBg.setCornerRadius(100f);
        barBg.setStroke(2, Color.parseColor("#E0E0E0"));
        bottomBar.setBackground(barBg);
        bottomBar.setElevation(20f);

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        barParams.setMargins(0, 0, 0, 60);
        mainRoot.addView(bottomBar, barParams);

        navOverview = createNavItem("概览", bottomBar);
        navModule = createNavItem("模块", bottomBar);
        navSettings = createNavItem("设置", bottomBar);

        navOverview.setOnClickListener(v -> switchPage(0));
        navModule.setOnClickListener(v -> switchPage(1));
        navSettings.setOnClickListener(v -> switchPage(2));

        setContentView(mainRoot);
        switchPage(0);
        updateStatusUI(false);
    }

    private void updateStatusUI(boolean active) {
        isActivated = active;
        if (statusCardView == null) return;

        if (active) {
            statusCardView.setBackground(createRoundRectBg(Color.parseColor("#E5F6E6"), 16f));
            tvStatusTitle.setText("已入局");
            tvStatusTitle.setTextColor(Color.parseColor("#2E7D32"));
            tvStatusSub.setText("已与 LSPosed 连通，设置可以落地。");
            tvCheckIcon.setVisibility(View.VISIBLE);
            if (xposedService != null && tvLsposedVer != null) {
                tvLsposedVer.setText("LSPosed " + xposedService.getFrameworkVersion());
            }
        } else {
            statusCardView.setBackground(createRoundRectBg(Color.parseColor("#F2F3F5"), 16f));
            tvStatusTitle.setText("未激活");
            tvStatusTitle.setTextColor(Color.parseColor("#333333"));
            tvStatusSub.setText("请在 LSPosed 中勾选本应用与系统框架。");
            tvCheckIcon.setVisibility(View.GONE);
        }
    }

    private GradientDrawable createRoundRectBg(int color, float radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        return shape;
    }

    private void switchPage(int index) {
        contentContainer.removeAllViews();
        navOverview.setTextColor(COLOR_TEXT_SUB);
        navModule.setTextColor(COLOR_TEXT_SUB);
        navSettings.setTextColor(COLOR_TEXT_SUB);

        if (index == 0) {
            contentContainer.addView(pageOverview);
            navOverview.setTextColor(COLOR_ACCENT);
        } else if (index == 1) {
            contentContainer.addView(pageModule);
            navModule.setTextColor(COLOR_ACCENT);
        } else if (index == 2) {
            contentContainer.addView(pageSettings);
            navSettings.setTextColor(COLOR_ACCENT);
        }
    }

    private TextView createNavItem(String text, LinearLayout parent) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(50, 15, 50, 15);
        parent.addView(tv);
        return tv;
    }

    private void initOverviewPage() {
        pageOverview = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageOverview.getChildAt(0)).getChildAt(0);

        TextView mainTitle = new TextView(this);
        mainTitle.setText("OriginBreaker");
        mainTitle.setTextColor(COLOR_TEXT_MAIN);
        mainTitle.setTextSize(32f);
        mainTitle.setTypeface(Typeface.create("serif", Typeface.BOLD));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, 20, 0, 30);
        mainTitle.setLayoutParams(titleParams);
        root.addView(mainTitle);

        FrameLayout statusFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frameParams.setMargins(0, 0, 0, 20);
        statusFrame.setLayoutParams(frameParams);

        statusCardView = new LinearLayout(this);
        statusCardView.setOrientation(LinearLayout.VERTICAL);
        statusCardView.setPadding(60, 60, 60, 60);
        statusCardView.setBackground(createRoundRectBg(Color.parseColor("#F2F3F5"), 16f));
        statusCardView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tvStatusTitle = new TextView(this);
        tvStatusTitle.setText("未激活");
        tvStatusTitle.setTextColor(Color.parseColor("#333333"));
        tvStatusTitle.setTextSize(24f);
        tvStatusTitle.setTypeface(Typeface.create("serif", Typeface.BOLD));

        tvStatusSub = new TextView(this);
        tvStatusSub.setText("请在 LSPosed 中勾选本应用与系统框架。");
        tvStatusSub.setTextColor(Color.parseColor("#888888"));
        tvStatusSub.setTextSize(14f);
        tvStatusSub.setPadding(0, 10, 0, 0);

        statusCardView.addView(tvStatusTitle);
        statusCardView.addView(tvStatusSub);

        tvCheckIcon = new TextView(this);
        tvCheckIcon.setText("✓");
        tvCheckIcon.setTextColor(Color.parseColor("#4CAF50"));
        tvCheckIcon.setTextSize(70f);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iconParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        iconParams.setMargins(0, 0, 40, 0);
        tvCheckIcon.setLayoutParams(iconParams);
        tvCheckIcon.setVisibility(View.GONE);

        statusFrame.addView(statusCardView);
        statusFrame.addView(tvCheckIcon);
        root.addView(statusFrame);

        LinearLayout infoCard = createListGroup();
        String appVer = "1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVer = pInfo.versionName;
        } catch (Exception ignored) {}

        addInfoItem(infoCard, "系统版本", Build.DISPLAY);
        addInfoItem(infoCard, "应用版本", appVer);
        addInfoItem(infoCard, "Android 版本", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        tvLsposedVer = addInfoItem(infoCard, "LSPosed 版本", "等待连接...");
        addInfoItem(infoCard, "设备型号", Build.MODEL);
        root.addView(infoCard);

        LinearLayout group2 = createListGroup();
        LinearLayout rowContainer = new LinearLayout(this);
        rowContainer.setOrientation(LinearLayout.HORIZONTAL);
        rowContainer.setGravity(Gravity.CENTER_VERTICAL);
        rowContainer.setPadding(50, 35, 50, 35);
        rowContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView reloadIcon = new TextView(this);
        reloadIcon.setText("↻");
        reloadIcon.setTextColor(COLOR_ACCENT);
        reloadIcon.setTextSize(30f);
        reloadIcon.setPadding(0, 0, 30, 0);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText("重载全部界面适配");
        label.setTextColor(COLOR_TEXT_MAIN);
        label.setTextSize(16f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView desc = new TextView(this);
        desc.setText("覆盖已授权的系统桌面、系统界面等");
        desc.setTextColor(COLOR_TEXT_SUB);
        desc.setTextSize(13f);
        desc.setPadding(0, 6, 0, 0);

        textLayout.addView(label);
        textLayout.addView(desc);

        rowContainer.addView(reloadIcon);
        rowContainer.addView(textLayout);

        rowContainer.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("选择重载方式")
                    .setMessage("【重启界面】：测试截屏、动画。\n【重启框架】：测试电池、温控。")
                    .setPositiveButton("重启界面", (dialog, which) -> runRootCommand("pkill -f com.android.systemui"))
                    .setNeutralButton("重启框架", (dialog, which) -> runRootCommand("killall system_server"))
                    .setNegativeButton("取消", null)
                    .show();
        });

        group2.addView(rowContainer);
        root.addView(group2);
    }

    private TextView addInfoItem(LinearLayout parent, String title, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(50, 25, 50, 25);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(COLOR_TEXT_MAIN);
        t.setTextSize(16f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(COLOR_TEXT_SUB);
        v.setTextSize(14f);
        v.setPadding(0, 8, 0, 0);

        row.addView(t);
        row.addView(v);
        parent.addView(row);
        return v;
    }

    private void initModulePage() {
        pageModule = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageModule.getChildAt(0)).getChildAt(0);

        TextView title = createTitle("功能模块");
        root.addView(title);

        LinearLayout group1 = createListGroup();
        addSwitchItem(group1, "游戏温控欺骗", "开启后系统读取假温度，拒绝降频", "thermal_spoof");
        addSwitchItem(group1, "强制解除截屏限制", "针对目标 App，强行解除禁止截图", "screen_snap");
        addSwitchItem(group1, "限制充电上限", "达到 80% 视为充满，减缓老化", "limit_charge");
        addSwitchItem(group1, "息屏断网休眠", "没收后台流氓唤醒锁，极速省电", "wakelock_killer");
        root.addView(group1);
    }

    private void initSettingsPage() {
        pageSettings = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageSettings.getChildAt(0)).getChildAt(0);

        TextView title = createTitle("设置");
        root.addView(title);

        LinearLayout group1 = createListGroup();
        addInputItem(group1, "系统动画速度", "默认 1.0 (越小越快)", "anim_scale");
        addInputItem(group1, "伪装温控数值", "建议 25.0 (摄氏度)", "thermal_temp");
        root.addView(group1);
    }

    private TextView createTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_TEXT_MAIN);
        title.setTextSize(32f);
        title.setTypeface(Typeface.create("serif", Typeface.BOLD));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 40);
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
        root.setPadding(50, 80, 50, 50);
        root.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollView.addView(root);
        frame.addView(scrollView);
        return frame;
    }

    private LinearLayout createListGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(COLOR_CARD);
        shape.setCornerRadius(40f);
        group.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 30);
        group.setLayoutParams(params);
        return group;
    }

    private LinearLayout createBaseItem(String labelText, String descText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(50, 35, 50, 35);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(COLOR_TEXT_MAIN);
        label.setTextSize(16f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView desc = new TextView(this);
        desc.setText(descText);
        desc.setTextColor(COLOR_TEXT_SUB);
        desc.setTextSize(13f);
        desc.setPadding(0, 6, 0, 0);

        textLayout.addView(label);
        textLayout.addView(desc);
        row.addView(textLayout);
        return row;
    }

    private void addSwitchItem(LinearLayout parent, String labelText, String descText, final String prefKey) {
        LinearLayout row = createBaseItem(labelText, descText);
        Switch switchView = new Switch(this);
        switchView.setChecked(prefs.getBoolean(prefKey, false));
        switchView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorStateList thumbStateList = new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{COLOR_ACCENT, Color.parseColor("#E0E0E0")}
            );
            try { switchView.setThumbTintList(thumbStateList); } catch (Throwable ignored) {}
        }

        switchView.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(prefKey, checked).apply();
            savePrefs();
        });

        row.addView(switchView);
        parent.addView(row);
    }

    private void addInputItem(LinearLayout parent, String labelText, String descText, final String prefKey) {
        LinearLayout row = createBaseItem(labelText, descText);

        TextView valueDisplay = new TextView(this);
        valueDisplay.setTextColor(COLOR_TEXT_SUB);
        valueDisplay.setTextSize(15f);
        String val = prefs.getString(prefKey, prefKey.equals("anim_scale") ? "1.0" : "25.0");
        valueDisplay.setText(val + " ›");
        row.addView(valueDisplay);

        row.setOnClickListener(v -> showInputDialog(prefKey, labelText, val, valueDisplay));
        parent.addView(row);
    }

    private void showInputDialog(String prefKey, String title, String currentValue, TextView valueDisplay) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(currentValue);
        input.setPadding(50, 50, 50, 50);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(50, 20, 50, 20);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                prefs.edit().putString(prefKey, newValue).apply();
                savePrefs();
                valueDisplay.setText(newValue + " ›");
                Toast.makeText(this, "保存成功，请重启生效", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void savePrefs() {
        try { Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml"); } catch (Exception ignored) {}
    }

    private void runRootCommand(String cmd) {
        Toast.makeText(this, "正在执行重载...", Toast.LENGTH_SHORT).show();
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