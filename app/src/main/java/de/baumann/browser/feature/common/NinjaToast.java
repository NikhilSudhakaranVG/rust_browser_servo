package de.baumann.browser.feature.common;

// Custom styled toast notifications for FOSS Browser.
// Provides various toast types with optional actions and better formatting.

import android.content.Context;
import android.widget.Toast;


public class NinjaToast {
    public static void show(Context context, int stringResId) {
        Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_SHORT).show();
    }
    public static void show(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}