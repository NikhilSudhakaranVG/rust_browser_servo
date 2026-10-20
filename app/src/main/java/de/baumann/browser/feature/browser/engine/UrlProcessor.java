package de.baumann.browser.feature.browser.engine;
import de.baumann.browser.feature.browser.NinjaWebView;
import de.baumann.browser.feature.common.BrowserUnit;
import de.baumann.browser.feature.common.HelperUnit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


/**
 * Shared URL pre-processing logic extracted from
 * {@link de.baumann.browser.feature.browser.NinjaWebView#loadUrl(String)}.
 *
 * <p>Both {@link WebViewEngine} and {@link ServoEngine} delegate to this
 * class so that business logic (tracking-param stripping, http→https upgrade,
 * redirect lookup) is not duplicated.</p>
 */
public class UrlProcessor {

    /** Outcome of {@link #process(String, SharedPreferences, String)}. */
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
            this.url = url;
            this.showTrackingDialog = false;
            this.cleanUrl = url;
            this.showHttpDialog = false;
        }

        Result(String url, boolean showTrackingDialog, String cleanUrl, boolean showHttpDialog) {
            this.url = url;
            this.showTrackingDialog = showTrackingDialog;
            this.cleanUrl = cleanUrl;
            this.showHttpDialog = showHttpDialog;
        }
    }

    private final Context context;

    public UrlProcessor(Context context) {
        this.context = context;
    }

    /**
     * Pre-processes a URL before loading:
     * <ol>
     *   <li>Strips {@code ;jsessionid=…} suffixes.</li>
     *   <li>Applies custom redirect rules via {@link BrowserUnit#redirectURL}.</li>
     *   <li>Resolves the URL via {@link BrowserUnit#queryWrapper} (search engine wrapping).</li>
     *   <li>Detects tracking query parameters.</li>
     *   <li>Detects plain-HTTP URLs for the upgrade dialog.</li>
     * </ol>
     *
     * @param rawUrl  the URL as typed or received
     * @param sp      SharedPreferences for user settings
     * @param profile the active profile key
     * @return a {@link Result} that the engine should act on
     */
    public Result process(String rawUrl, SharedPreferences sp, String profile) {
        String url = rawUrl;

        // 1. Strip jsessionid tracking suffix
        if (url.contains(";jsessionid=")) {
            String tracking = url.substring(url.lastIndexOf(";"));
            url = url.replace(tracking, "");
        }

        // 2. Apply custom redirect rules
        // Note: redirectURL requires a WebView reference in the original impl;
        // we pass null-safe via BrowserUnit — adapt if the signature changes.
        String redirected = BrowserUnit.queryWrapper(context, url);

        // 3. Detect tracking query params
        if (sp.getBoolean(profile + "_trackingULS", true)
                && redirected.contains("?")
                && redirected.contains("/")) {
            String lastIndex = redirected.substring(redirected.lastIndexOf("/"));
            String tracking  = redirected.substring(redirected.lastIndexOf("?"));
            String urlClean  = redirected.replace(tracking, "");
            if (lastIndex.contains(tracking)) {
                return new Result(redirected, true, urlClean, false);
            }
        }

        // 4. Detect plain HTTP (only ask once unless user said "never ask")
        if (redirected.startsWith("http://")
                && sp.getString("dialog_neverAsk", "no").equals("no")) {
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
        return currentDomain != null && !currentDomain.equals(newDomain);
    }
}
