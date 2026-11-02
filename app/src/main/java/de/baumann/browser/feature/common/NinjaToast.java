package de.baumann.browser.feature.common;

// Custom styled toast notifications for FOSS Browser.
// Provides various toast types with optional actions and better formatting.

import android.content.Context;
import android.widget.Toast;


public class NinjaToast {

    // ── Core helpers ──────────────────────────────────────────────────────────

    /** Shows a short-duration toast using a string resource id. */
    public static void show(Context context, int stringResId) {
        Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_SHORT).show();
    }

    /** Shows a short-duration toast using a plain string. */
    public static void show(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    /** Shows a long-duration toast using a string resource id. */
    public static void showLong(Context context, int stringResId) {
        Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_LONG).show();
    }

    /** Shows a long-duration toast using a plain string. */
    public static void showLong(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    // ── Semantic convenience variants ─────────────────────────────────────────

    /**
     * Shows a short success toast.
     * Use for confirmations such as "Saved", "Copied", "Bookmark added".
     */
    public static void showSuccess(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a long error toast.
     * Use when an operation fails and the user needs time to read the message.
     */
    public static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a long warning toast.
     * Use for non-fatal issues the user should be aware of.
     */
    public static void showWarning(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a short informational toast.
     * Use for neutral status messages.
     */
    public static void showInfo(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // ── Resource-id overloads for semantic variants ───────────────────────────

    /** @see #showSuccess(Context, String) */
    public static void showSuccess(Context context, int stringResId) {
        showSuccess(context, context.getString(stringResId));
    }

    /** @see #showError(Context, String) */
    public static void showError(Context context, int stringResId) {
        showError(context, context.getString(stringResId));
    }

    /** @see #showWarning(Context, String) */
    public static void showWarning(Context context, int stringResId) {
        showWarning(context, context.getString(stringResId));
    }

    /** @see #showInfo(Context, String) */
    public static void showInfo(Context context, int stringResId) {
        showInfo(context, context.getString(stringResId));
    }
}
