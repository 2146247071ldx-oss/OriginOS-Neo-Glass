package com.neoglass.originos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private boolean isActivated = false;
    
    private int COLOR_BG, COLOR_CARD, COLOR_TEXT_MAIN, COLOR_TEXT_SUB, COLOR_ACCENT;
    private int COLOR_STATUS_BG, COLOR_STATUS_TEXT, COLOR_BOTTOM_BAR;
    
    private FrameLayout contentContainer;
    private LinearLayout pageOverview, pageModule, pageSettings;
    private TextView navOverview, navModule, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);
        try {
            Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml");
        } catch (Exception ignored) {}

        isActivated = checkActivation();
        initColors();

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
        bottomBar.setPadding(30, 20, 30, 20);
        
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(COLOR_BOTTOM_BAR);
        barBg.setCornerRadius(100f); 
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
    }

    private boolean checkActivation() {
        try {
            Process p = Runtime.getRuntime().exec("su -c ls /data/local/tmp/origin_breaker_active");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return line != null && line.contains("origin_breaker_active");
        } catch (Exception e) {
            return false;
        }
    }

    private void initColors() {
        boolean useDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (useDark) {
            COLOR_BG = Color.parseColor("#121212"); COLOR_CARD = Color.parseColor("#1E1E1E");
            COLOR_TEXT_MAIN = Color.parseColor("#EAEAEA"); COLOR_TEXT_SUB = Color.parseColor("#888888");
            COLOR_ACCENT = Color.parseColor("#4CAF50");
            COLOR_STATUS_BG = isActivated ? Color.parseColor("#1B2B1B") : Color.parseColor("#2A2A2A");
            COLOR_STATUS_TEXT = isActivated ? Color.parseColor("#81C784") : Color.parseColor("#888888");
            COLOR_BOTTOM_BAR = Color.parseColor("#2A2A2E");
        } else {
            COLOR_BG = Color.parseColor("#F3F4F9"); COLOR_CARD = Color.parseColor("#FFFFFF");
            COLOR_TEXT_MAIN = Color.parseColor("#000000"); COLOR_TEXT_SUB = Color.parseColor("#888888");
            COLOR_ACCENT = Color.parseColor("#3B82F6"); 
            COLOR_STATUS_BG = isActivated ? Color.parseColor("#E8F5E9") : Color.parseColor("#E5E5EA");
            COLOR_STATUS_TEXT = isActivated ? Color.parseColor("#2E7D32") : Color.parseColor("#888888");
            COLOR_BOTTOM_BAR = Color.parseColor("#FFFFFF");
        }
    }

    private void switchPage(int index) {
        contentContainer.removeAllViews();
        navOverview.setAlpha(0.5f); navModule.setAlpha(0.5f); navSettings.setAlpha(0.5f);
        if (index == 0) { contentContainer.addView(pageOverview); navOverview.setAlpha(1f); }
        else if (index == 1) { contentContainer.addView(pageModule); navModule.setAlpha(1f); }
        else if (index == 2) { contentContainer.addView(pageSettings); navSettings.setAlpha(1f); }
    }

    private TextView createNavItem(String text, LinearLayout parent) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextSize(16f);
        tv.setTypeface(Typeface.create("serif", Typeface.BOLD));
        tv.setTextColor(COLOR_TEXT_MAIN);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(60, 20, 60, 20);
        parent.addView(tv);
        return tv;
    }

    private void initOverviewPage() {
        pageOverview = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageOverview.getChildAt(0)).getChildAt(0);

        TextView title = createTitle("OriginBreaker");
        root.addView(title);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(60, 60, 60, 60);
        GradientDrawable statusShape = new GradientDrawable();
        statusShape.setColor(COLOR_STATUS_BG);
        statusShape.setCornerRadius(60f);
        statusCard.setBackground(statusShape);
        
        TextView statusTitle = new TextView(this);
        statusTitle.setText(isActivated ? "已入局" : "尚未相逢");
        statusTitle.setTextColor(COLOR_STATUS_TEXT);
        statusTitle.setTextSize(28f);
        statusTitle.setTypeface(Typeface.create("serif", Typeface.BOLD));
        
        TextView statusSub = new TextView(this);
        statusSub.setText(isActivated ? "已与 LSPosed 连通，设置可以落地。" : "框架服务还没接上，稍后再看一眼。");
        statusSub.setTextColor(COLOR_STATUS_TEXT);
        statusSub.setTextSize(14f);
        statusSub.setAlpha(0.7f);
        statusSub.setPadding(0, 10, 0, 40);
        
        TextView statusBottom = new TextView(this);
        statusBottom.setText(isActivated ? "此身已在局中" : "缘分未到，框架未现");
        statusBottom.setTextColor(COLOR_STATUS_TEXT);
        statusBottom.setTextSize(14f);
        statusBottom.setTypeface(Typeface.create("serif", Typeface.BOLD));
        
        statusCard.addView(statusTitle);
        statusCard.addView(statusSub);
        statusCard.addView(statusBottom);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 0, 0, 40);
        statusCard.setLayoutParams(statusParams);
        root.addView(statusCard);

        LinearLayout infoCard = createListGroup();
        String appVer = "1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVer = pInfo.versionName;
        } catch (Exception ignored) {}

        addInfoItem(infoCard, "系统版本", Build.DISPLAY);
        addInfoItem(infoCard, "应用版本", appVer);
        addInfoItem(infoCard, "Android 版本", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addInfoItem(infoCard, "LSPosed 版本", isActivated ? "已连接" : "未连接");
        addInfoItem(infoCard, "构建时间", "2026-10-22");
        addInfoItem(infoCard, "设备型号", Build.MODEL);
        root.addView(infoCard);
    }

    private void addInfoItem(LinearLayout parent, String title, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(60, 30, 60, 30);

        TextView t = new TextView(this);
        t.setText(title); t.setTextColor(COLOR_TEXT_MAIN);
        t.setTextSize(18f); t.setTypeface(Typeface.create("serif", Typeface.BOLD));

        TextView v = new TextView(this);
        v.setText(value); v.setTextColor(COLOR_TEXT_SUB);
        v.setTextSize(14f); v.setPadding(0, 8, 0, 0);

        row.addView(t); row.addView(v);
        parent.addView(row);
    }

    private void initModulePage() {
        pageModule = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageModule.getChildAt(0)).getChildAt(0);

        TextView title = createTitle("功能模块");
        root.addView(title);

        LinearLayout group1 = createListGroup();
        addSwitchItem(group1, "游戏温控欺骗", "开启后系统读取假温度，拒绝降频", "thermal_spoof");
        addSwitchItem(group1, "强制解除截屏限制", "针对目标 App，强行解除禁止截图", "screen_snap");
        root.addView(group1);

        LinearLayout group2 = createListGroup();
        addSwitchItem(group2, "限制充电上限", "达到 80% 视为充满，减缓老化", "limit_charge");
        addSwitchItem(group2, "息屏断网休眠", "没收后台流氓唤醒锁，极速省电", "wakelock_killer");
        root.addView(group2);
        
        LinearLayout group3 = createListGroup();
        LinearLayout actionRow = createBaseItem("重载全部界面适配", "点击生效底层更改（温控/电池/UI）");
        actionRow.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("选择重载方式")
                .setMessage("【重启界面】：测试截屏、动画。\n【重启框架】：测试电池、温控。")
                .setPositiveButton("重启界面", (dialog, which) -> runRootCommand("pkill -f com.android.systemui"))
                .setNeutralButton("重启框架", (dialog, which) -> runRootCommand("killall system_server"))
                .setNegativeButton("取消", null)
                .show();
        });
        group3.addView(actionRow);
        root.addView(group3);
    }

    private void initSettingsPage() {
        pageSettings = createBaseScroll();
        LinearLayout root = (LinearLayout) ((ScrollView) pageSettings.getChildAt(0)).getChildAt(0);

        TextView title = createTitle("主题设置");
        root.addView(title);

        LinearLayout modeBar = new LinearLayout(this);
        modeBar.setOrientation(LinearLayout.HORIZONTAL);
        modeBar.setGravity(Gravity.CENTER_VERTICAL);
        modeBar.setPadding(0, 0, 0, 40);

        String[] modes = {"跟随系统", "浅色", "深色"};
        int currentMode = prefs.getInt("theme_mode", 0);

        for (int i = 0; i < 3; i++) {
            TextView btn = new TextView(this);
            btn.setText(modes[i]);
            btn.setTextSize(14f);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, 20, 0, 20);
            btn.setTypeface(Typeface.create("serif", Typeface.BOLD));
            
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(30f);
            if (currentMode == i) {
                bg.setColor(COLOR_CARD);
                btn.setTextColor(COLOR_TEXT_MAIN);
                btn.setElevation(5f);
            } else {
                bg.setColor(Color.parseColor("#00000000")); 
                btn.setTextColor(COLOR_TEXT_SUB);
            }
            btn.setBackground(bg);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p.setMargins(10, 0, 10, 0);
            btn.setLayoutParams(p);
            
            final int modeIndex = i;
            btn.setOnClickListener(v -> {
                prefs.edit().putInt("theme_mode", modeIndex).apply();
                recreate(); 
            });
            modeBar.addView(btn);
        }
        root.addView(modeBar);

        LinearLayout group1 = createListGroup();
        addInputItem(group1, "系统动画速度", "默认 1.0 (越小越快)", "anim_scale");
        addInputItem(group1, "伪装温控数值", "建议 25.0 (摄氏度)", "thermal_temp");
        root.addView(group1);
        
        LinearLayout group2 = createListGroup();
        addFakeSwitchItem(group2, "模糊", "启用顶栏和底栏的模糊效果");
        addFakeSwitchItem(group2, "悬浮底栏", "使用 Apple 风格的悬浮底栏");
        addFakeSwitchItem(group2, "液态玻璃", "启用悬浮底栏的液态玻璃效果");
        root.addView(group2);
    }

    private TextView createTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_TEXT_MAIN);
        title.setTextSize(40f);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 50);
        title.setLayoutParams(p);
        return title;
    }

    private LinearLayout createBaseScroll() {
        LinearLayout frame = new LinearLayout(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 120, 60, 100);
        scrollView.addView(root);
        frame.addView(scrollView);
        return frame;
    }

    private LinearLayout createListGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(COLOR_CARD);
        shape.setCornerRadius(50f);
        group.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 30);
        group.setLayoutParams(params);
        return group;
    }

    private LinearLayout createBaseItem(String labelText, String descText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(60, 50, 60, 50);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        TextView label = new TextView(this);
        label.setText(labelText); label.setTextColor(COLOR_TEXT_MAIN);
        label.setTextSize(18f); label.setTypeface(Typeface.create("serif", Typeface.BOLD));

        TextView desc = new TextView(this);
        desc.setText(descText); desc.setTextColor(COLOR_TEXT_SUB);
        desc.setTextSize(14f); desc.setPadding(0, 8, 0, 0);

        textLayout.addView(label); textLayout.addView(desc);
        row.addView(textLayout);
        return row;
    }

    private void addSwitchItem(LinearLayout parent, String labelText, String descText, final String prefKey) {
        LinearLayout row = createBaseItem(labelText, descText);
        Switch switchView = new Switch(this);
        switchView.setChecked(prefs.getBoolean(prefKey, false));
        
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

    private void addFakeSwitchItem(LinearLayout parent, String labelText, String descText) {
        LinearLayout row = createBaseItem(labelText, descText);
        Switch switchView = new Switch(this);
        switchView.setChecked(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorStateList thumbStateList = new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{COLOR_ACCENT, Color.parseColor("#E0E0E0")}
            );
            try { switchView.setThumbTintList(thumbStateList); } catch (Throwable ignored) {}
        }
        row.addView(switchView);
        parent.addView(row);
    }

    private void addInputItem(LinearLayout parent, String labelText, String descText, final String prefKey) {
        LinearLayout row = createBaseItem(labelText, descText);
        
        TextView valueDisplay = new TextView(this);
        valueDisplay.setTextColor(COLOR_TEXT_SUB);
        valueDisplay.setTextSize(16f);
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