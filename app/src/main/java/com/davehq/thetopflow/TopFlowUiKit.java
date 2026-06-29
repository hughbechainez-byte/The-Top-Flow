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
    static final int INDIGO = Color.rgb(8, 10, 14);
    static final int PANEL = Color.rgb(10, 13, 18);
    static final int RAISED = Color.rgb(16, 21, 28);
    static final int MINT = Color.rgb(90, 215, 160);
    static final int TEXT = Color.rgb(248, 249, 255);
    static final int TEXT_SOFT = Color.rgb(184, 192, 218);
    static final int HAIRLINE = Color.argb(64, 77, 219, 255);

    private TopFlowUiKit() {}

    static GradientDrawable floatingPanel(Context context, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(234, 12, 16, 22), Color.argb(234, 2, 3, 6)}
        );
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), HAIRLINE);
        return drawable;
    }

    static GradientDrawable oledSurface(Context context, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{OLED, OLED}
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
        return fontForEditor(id, Typeface.NORMAL);
    }

    static Typeface fontForEditor(String id, int style) {
        if ("slim".equals(id)) return Typeface.create("sans-serif-light", style);
        if ("pixel".equals(id)) return Typeface.create(Typeface.MONOSPACE, style);
        if ("terminal".equals(id)) return Typeface.create("monospace", style);
        if ("rounded".equals(id)) return Typeface.create("sans-serif-medium", style);
        if ("serif".equals(id)) return Typeface.create(Typeface.SERIF, style);
        if ("monospace".equals(id)) return Typeface.create(Typeface.MONOSPACE, style);
        if ("casual".equals(id)) return Typeface.create("casual", style);
        if ("cursive".equals(id)) return Typeface.create("cursive", style);
        return Typeface.create("sans-serif", style);
    }

    static String[] fontPreviewIds() {
        return new String[]{"sans", "slim", "pixel", "terminal", "rounded", "serif", "monospace"};
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
