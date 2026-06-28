package com.davehq.thetopflow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

final class TopFlowUiKit {
    static final int OLED = Color.BLACK;
    static final int INDIGO = Color.rgb(45, 52, 88);
    static final int PANEL = Color.rgb(52, 59, 100);
    static final int RAISED = Color.rgb(68, 76, 128);
    static final int MINT = Color.rgb(90, 215, 160);
    static final int TEXT = Color.rgb(248, 249, 255);
    static final int TEXT_SOFT = Color.rgb(184, 192, 218);
    static final int HAIRLINE = Color.argb(64, 77, 219, 255);

    private TopFlowUiKit() {}

    static GradientDrawable floatingPanel(Context context, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(232, 58, 65, 110), Color.argb(232, 38, 45, 78)}
        );
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), HAIRLINE);
        return drawable;
    }

    static GradientDrawable oledSurface(Context context, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(11, 15, 29), OLED}
        );
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), Color.argb(48, 90, 215, 160));
        return drawable;
    }

    static GradientDrawable mintPill(Context context) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(74, 205, 151), Color.rgb(105, 225, 175)}
        );
        drawable.setCornerRadius(dp(context, 999));
        return drawable;
    }

    static void applyFloating(View view, int elevationDp) {
        if (view == null) return;
        view.setElevation(dp(view.getContext(), elevationDp));
        view.setClipToOutline(false);
    }

    static void applyQuietText(TextView view) {
        if (view == null) return;
        view.setTextColor(TEXT_SOFT);
        view.setIncludeFontPadding(false);
        view.setLetterSpacing(0f);
    }

    static RippleDrawable ripple(int accent) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(54, Color.red(accent), Color.green(accent), Color.blue(accent))), null, null);
    }

    static Typeface fontForPreview(String id) {
        if ("serif".equals(id)) return Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        if ("monospace".equals(id)) return Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
        if ("casual".equals(id)) return Typeface.create("casual", Typeface.NORMAL);
        if ("cursive".equals(id)) return Typeface.create("cursive", Typeface.NORMAL);
        return Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    static String[] fontPreviewIds() {
        return new String[]{"sans", "serif", "monospace", "casual", "cursive"};
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
