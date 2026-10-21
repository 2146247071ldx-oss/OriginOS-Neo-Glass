
package com.neoglass.originos;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable glassBackground() {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {
                    Color.rgb(65, 83, 112),
                    Color.rgb(31, 39, 57)
                });
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), 0x66FFFFFF);
        return bg;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(17, 22, 34));

        TextView title = new TextView(this);
        title.setText("NEO GLASS");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText(
            "OriginOS 6 · 半透明玻璃风\\n\\n" +
            "时钟 · 通知 · 控制中心 · 动画\\n\\n" +
            "当前为模块开发预览版\\n" +
            "系统界面 Hook 尚待适配和测试"
        );
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFFDCE6F5);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(dp(12), dp(24), dp(12), dp(24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(28), dp(24), dp(28));
        card.setBackground(glassBackground());

        card.addView(title);
        card.addView(subtitle);

        root.addView(card, new LinearLayout.LayoutParams(
            -1, -2
        ));

        setContentView(root);
    }
}
