package com.neoglass.originos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("NeoGlassPrefs", Context.MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        
        // ==========================================
        // 这里就是你要找的背景代码！我写成了深邃的极光渐变
        // ==========================================
        GradientDrawable bgGradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, 
                new int[]{Color.parseColor("#0F2027"), Color.parseColor("#203A43"), Color.parseColor("#2C5364")}
        );
        scrollView.setBackground(bgGradient);
        
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(60, 120, 60, 120);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        scrollView.addView(rootLayout);

        // 液态玻璃卡片容器
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(60, 60, 60, 60);
        cardLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable glassDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#33FFFFFF"), Color.parseColor("#1AFFFFFF")}
        );
        glassDrawable.setCornerRadius(60f);
        glassDrawable.setStroke(2, Color.parseColor("#40FFFFFF"));
        cardLayout.setBackground(glassDrawable);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLayout.setLayoutParams(cardParams);

        // 标题
        TextView title = new TextView(this);
        title.setText("NEO GLASS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        cardLayout.addView(title);

        // 副标题（已修复之前的 \n 问题）
        TextView subtitle = new TextView(this);
        subtitle.setText("OriginOS 6 · 半透明玻璃风\n时钟 · 通知 · 控制中心 · 动画");
        subtitle.setTextColor(Color.parseColor("#B3FFFFFF"));
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 60);
        cardLayout.addView(subtitle);

        // 添加四个开关
        addSwitch(cardLayout, "时钟玻璃美化", "clock");
        addSwitch(cardLayout, "通知界面美化", "notification");
        addSwitch(cardLayout, "控制中心美化", "control");
        addSwitch(cardLayout, "动画效果美化", "animation");

        // 底部作者链接
        TextView author = new TextView(this);
        author.setText("开发预览版 | 酷安: @林东旭"); 
        author.setTextColor(Color.parseColor("#8099B2")); 
        author.setTextSize(14f);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 60, 0, 20);
        
        author.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/39927079"));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cardLayout.addView(author);

        rootLayout.addView(cardLayout);
        setContentView(scrollView);
    }

    private void addSwitch(LinearLayout parent, String labelText, final String prefKey) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 30, 0, 30);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(labelParams);

        Switch switchView = new Switch(this);
        boolean isChecked = prefs.getBoolean(prefKey, false);
        switchView.setChecked(isChecked);
        
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(prefKey, isChecked).apply();
            }
        });

        row.addView(label);
        row.addView(switchView);
        parent.addView(row);
    }
}