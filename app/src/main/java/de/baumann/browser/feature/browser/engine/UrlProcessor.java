package de.baumann.browser.feature.browser.engine;

import de.baumann.browser.feature.common.BrowserUnit;
import de.baumann.browser.feature.common.HelperUnit;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Shared URL pre-processing logic extracted from
 * {@link de.baumann.browser.feature.browser.NinjaWebView#loadUrl(String)}.
 *
 * <p>Both {@link WebViewEngine} and {@link ServoEngine} delegate to this
 * class so that business logic (tracking-param stripping, http→https upgrade,
 * redirect lookup) is not duplicated.</p>
 */
public class UrlProcessor {

    /** Outcome of {@link #process(String, SharedPreferences, String, boolean)}. */
    public static final class Result {
        /** The final, cleaned URL to load. Never null. */
        public final String url;
        /** If true, the caller should show the tracking-params dialog. */
        public final boolean showTrackingDialog;
        /** The URL with tracking params stripped (only set when {@link #showTrackingDialog} is true). */
        public final String cleanUrl;
        /** If true, the caller should show the http→https upgrade dialog. */
        public final boolean showHttpDialog;

        Result(String url) {
            this.url = url != null ? url : "";
            this.showTrackingDialog = false;
            this.cleanUrl = this.url;
            this.showHttpDialog = false;
        }

        Result(String url, boolean showTrackingDialog, String cleanUrl, boolean showHttpDialog) {
            this.url = url != null ? url : "";
            this.showTrackingDialog = showTrackingDialog;
            this.cleanUrl = cleanUrl != null ? cleanUrl : this.url;
            this.showHttpDialog = showHttpDialog;
        }
    }

    private final Context context;

    public UrlProcessor(Context context) {
        this.context = context;
    }

    /**
     * Pre-processes a URL before loading, defaulting to showing dialogs if applicable.
     */
    public Result process(String rawUrl, SharedPreferences sp, String profile) {
        return process(rawUrl, sp, profile, false);
    }

    /**
     * Pre-processes a URL before loading:
     * <ol>
     *   <li>Bypasses special non-web schemes (data:, blob:, file:, about:, javascript:, etc.).</li>
     *   <li>Strips {@code ;jsessionid=…} suffixes safely while preserving query and fragments.</li>
     *   <li>Resolves search queries via {@link BrowserUnit#queryWrapper}.</li>
     *   <li>Detects tracking query parameters (if not skipped).</li>
     *   <li>Detects plain-HTTP URLs for the upgrade dialog (if not skipped).</li>
     * </ol>
     *
     * @param rawUrl      the URL as typed or received
     * @param sp          SharedPreferences for user settings
     * @param profile     the active profile key
     * @param skipDialogs true if confirmation dialogs should be bypassed
     * @return a {@link Result} that the engine should act on
     */
    public Result process(String rawUrl, SharedPreferences sp, String profile, boolean skipDialogs) {
        if (rawUrl == null) {
            return new Result("");
        }

        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return new Result("");
        }

        // Fast-path for non-HTTP/non-web schemes
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("about:") || lower.startsWith("data:")
                || lower.startsWith("blob:") || lower.startsWith("file:")
                || lower.startsWith("javascript:") || lower.startsWith("content:")
                || lower.startsWith("mailto:") || lower.startsWith("tel:")
                || lower.startsWith("intent:") || lower.startsWith("foss://")) {
            return new Result(trimmed);
        }

        String url = trimmed;

        // 1. Strip jsessionid tracking suffix safely (preserve query and fragment)
        if (url.contains(";jsessionid=")) {
            url = url.replaceAll(";jsessionid=[^?#]*", "");
        }

        // 2. Resolve URL or wrap search query
        boolean isRawUrlAUrl = BrowserUnit.isURL(url);
        String redirected = BrowserUnit.queryWrapper(context, url);

        if (skipDialogs) {
            return new Result(redirected);
        }

        // 3. Detect tracking query params (default is false; only for actual URL navigations, not search queries)
        if (isRawUrlAUrl && sp != null
                && sp.getBoolean(profile + "_trackingULS", false)
                && redirected.contains("?")
                && redirected.contains("/")) {
            int qIdx = redirected.indexOf('?');
            String tracking = redirected.substring(qIdx);
            String urlClean = redirected.substring(0, qIdx);
            int lastSlash = redirected.lastIndexOf('/');
            if (lastSlash < qIdx && tracking.length() > 1) {
                return new Result(redirected, true, urlClean, false);
            }
        }

        // 4. Detect plain HTTP (only ask once unless user said "never ask")
        if (redirected.startsWith("http://")
                && sp != null
                && "no".equals(sp.getString("dialog_neverAsk", "no"))) {
            return new Result(redirected, false, redirected, true);
        }

        return new Result(redirected);
    }

    /**
     * Returns true if the given URL crosses a domain boundary relative to
     * the current URL, indicating a profile reset may be appropriate.
     */
    public static boolean isCrossDomain(String currentUrl, String newUrl) {
        if (currentUrl == null || newUrl == null) return false;
        String currentDomain = HelperUnit.domain(currentUrl);
        String newDomain     = HelperUnit.domain(newUrl);
        if (currentDomain == null || newDomain == null) return false;
        if (currentDomain.isEmpty() || newDomain.isEmpty()) return false;
        return !currentDomain.equalsIgnoreCase(newDomain);
    }
}
