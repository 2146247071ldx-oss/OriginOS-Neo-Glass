package com.neoglass.originos;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        rootLayout.setPadding(50, 100, 50, 100);
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
        title.setText("Origin Cyber");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        cardLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("手感引擎 · 赛博弹幕");
        subtitle.setTextColor(Color.parseColor("#E6FFFFFF"));
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 50);
        cardLayout.addView(subtitle);

        // 弹簧动画开关
        addSwitch(cardLayout, "全局 Q 弹动画", "spring_anim", "替换底层插值器，带来果冻般跟手感");

        // 弹幕输入框
        TextView label = new TextView(this);
        label.setText("状态栏赛博弹幕文字:");
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, 30, 0, 10);
        cardLayout.addView(label);

        EditText input = new EditText(this);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#80FFFFFF"));
        input.setHint("输入你想展示的文字...");
        input.setText(prefs.getString("ticker_text", "Origin Cyber 极客专属"));
        
        // 输入框的毛玻璃背景
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#26FFFFFF"));
        inputBg.setCornerRadius(20f);
        input.setBackground(inputBg);
        input.setPadding(30, 20, 30, 20);
        cardLayout.addView(input);

        // 监听文字改变并保存
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefs.edit().putString("ticker_text", s.toString()).apply();
                try { Runtime.getRuntime().exec("su -c chmod 644 /data/data/com.neoglass.originos/shared_prefs/NeoGlassPrefs.xml"); } catch (Exception ignored) {}
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 跑马灯开关
        addSwitch(cardLayout, "状态栏跑马灯", "ticker_on", "将文字强行插入状态栏左侧滚动显示");

        TextView warn = new TextView(this);
        warn.setText("⚠️ 需在 LSPosed 中勾选“系统界面”并重启");
        warn.setTextColor(Color.parseColor("#B3FFD700"));
        warn.setTextSize(12f);
        warn.setGravity(Gravity.CENTER);
        warn.setPadding(0, 30, 0, 0);
        cardLayout.addView(warn);

        // 酷安链接
        TextView author = new TextView(this);
        author.setText("极客开发 | 酷安: @林东旭"); 
        author.setTextColor(Color.parseColor("#80FFFFFF")); 
        author.setTextSize(13f);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 40, 0, 10);
        author.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.coolapk.com/u/39927079")));
            } catch (Exception ignored) {}
        });
        cardLayout.addView(author);

        rootLayout.addView(cardLayout);
        setContentView(frameLayout);
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
        switchView.setChecked(prefs.getBoolean(prefKey, false));
        
        try {
            switchView.setThumbTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.parseColor("#26D0CE"), Color.parseColor("#80FFFFFF")}));
            switchView.setTrackTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.parseColor("#4DFFFFFF"), Color.parseColor("#26FFFFFF")}));
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