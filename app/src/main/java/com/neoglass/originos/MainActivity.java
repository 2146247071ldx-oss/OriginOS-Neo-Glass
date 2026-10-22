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

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private boolean isActivated = false;
    
    private int COLOR_BG = Color.parseColor("#F2F4F7");
    private int COLOR_CARD = Color.parseColor("#FFFFFF");
    private int COLOR_TEXT_MAIN = Color.parseColor("#111111");
    private int COLOR_TEXT_SUB = Color.parseColor("#777777");
    private int COLOR_ACCENT = Color.parseColor("#3B82F6");
    private int COLOR_BOTTOM_BAR = Color.parseColor("#FFFFFF");

    private FrameLayout contentContainer;
    private LinearLayout pageOverview, pageModule, pageSettings;
    private TextView navOverview, navModule, navSettings;

    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);
        try {
            Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml");
        } catch (Exception ignored) {}

        isActivated = isModuleActive();

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

        TextView title = createTitle("概览");
        root.addView(title);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(50, 50, 50, 50);
        
        GradientDrawable statusShape = new GradientDrawable();
        statusShape.setColor(isActivated ? Color.parseColor("#E8F5E9") : Color.parseColor("#E5E5EA"));
        statusShape.setCornerRadius(40f);
        statusCard.setBackground(statusShape);
        statusCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        TextView statusTitle = new TextView(this);
        statusTitle.setText(isActivated ? "已激活" : "未激活");
        statusTitle.setTextColor(isActivated ? Color.parseColor("#2E7D32") : Color.parseColor("#888888"));
        statusTitle.setTextSize(24f);
        statusTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        
        TextView statusSub = new TextView(this);
        statusSub.setText(isActivated ? "LSPosed 框架已连通，功能生效中。" : "请在 LSPosed 中勾选本应用与系统框架。");
        statusSub.setTextColor(isActivated ? Color.parseColor("#388E3C") : Color.parseColor("#888888"));
        statusSub.setTextSize(14f);
        statusSub.setPadding(0, 10, 0, 0);
        
        statusCard.addView(statusTitle);
        statusCard.addView(statusSub);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 0, 0, 30);
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
        addInfoItem(infoCard, "设备型号", Build.MODEL);
        root.addView(infoCard);
    }

    private void addInfoItem(LinearLayout parent, String title, String value) {
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
        
        LinearLayout group2 = createListGroup();
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
        group2.addView(actionRow);
        root.addView(group2);
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
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
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
        root.setPadding(50, 100, 50, 50);
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