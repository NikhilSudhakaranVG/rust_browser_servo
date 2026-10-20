package de.baumann.browser.feature.browser.engine;

import de.baumann.browser.feature.browser.AdBlock;
import de.baumann.browser.feature.browser.List_standard;
import de.baumann.browser.feature.common.HelperUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.print.PrintDocumentAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.util.Log;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link BrowserEngine} implementation backed by Android's {@link WebView}.
 *
 * <p>This adapter wraps the existing WebView + WebViewClient + WebChromeClient
 * stack so that the host can interact with it through the engine-agnostic
 * {@link BrowserEngine} interface. The goal is zero behaviour change
 * relative to the original implementation while optimizing performance,
 * memory, thread safety, and resource handling.</p>
 */
public class WebViewEngine implements BrowserEngine {

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static String sCachedDefaultUserAgent = null;

    // ── Fields ────────────────────────────────────────────────────────────

    private final Context context;
    private final WebView webView;
    private BrowserEngineClient client;
    private final AdBlock adBlock;
    private final List_standard listStandard;
    private final UrlProcessor urlProcessor;

    private volatile boolean adBlockEnabled = true;
    private boolean isIncognito = false;
    private boolean loading = false;
    private boolean stopped = false;

    // ── Constructor ───────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    public WebViewEngine(Context context) {
        this.context = context;
        this.webView = new WebView(context);
        this.adBlock = new AdBlock(context);
        this.listStandard = new List_standard(context);
        this.urlProcessor = new UrlProcessor(context);

        webView.setWebViewClient(buildWebViewClient());
        webView.setWebChromeClient(buildWebChromeClient());
        webView.setDownloadListener(buildDownloadListener());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setSafeBrowsingEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
    }

    // ── BrowserEngine: View ──────────────────────────────────────────────

    @Override
    public View getView() {
        return webView;
    }

    public WebView getWebView() {
        return webView;
    }

    // ── BrowserEngine: Navigation ─────────────────────────────────────────

    @Override
    public void loadUrl(String url) {
        loadUrl(url, false);
    }

    @Override
    public void loadUrl(String url, boolean skipDialogs) {
        stopped = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");

        // Profile reset on domain change
        if (UrlProcessor.isCrossDomain(webView.getUrl(), url)
                && sp.getBoolean("sp_standard_always", true)) {
            sp.edit().putString("profile", "profileStandard").apply();
            profile = "profileStandard";
        }

        // Whitelist overrides domain profile
        if (listStandard != null && listStandard.isWhite(url)) {
            profile = HelperUnit.domain(url);
        }

        UrlProcessor.Result result = urlProcessor.process(url, sp, profile, skipDialogs);

        if (!skipDialogs && (result.showTrackingDialog || result.showHttpDialog)) {
            if (result.showTrackingDialog && client != null) {
                stopped = true;
                loading = false;
                webView.stopLoading();
                client.shouldOverrideUrlLoading("internal://tracking-dialog?url="
                        + Uri.encode(result.url)
                        + "&clean=" + Uri.encode(result.cleanUrl));
                return;
            }
            if (result.showHttpDialog && client != null) {
                stopped = true;
                loading = false;
                webView.stopLoading();
                client.shouldOverrideUrlLoading("internal://http-dialog?url="
                        + Uri.encode(result.url));
                return;
            }
        }

        applyPreferences(sp, profile, result.url);
        webView.loadUrl(result.url, getRequestHeaders(sp, profile, result.url));
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        stopped = false;
        webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public void reload() {
        stopped = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");
        String currentUrl = webView.getUrl();
        if (listStandard != null && listStandard.isWhite(currentUrl)) {
            profile = HelperUnit.domain(currentUrl);
        }
        applyPreferences(sp, profile, currentUrl);
        webView.reload();
    }

    @Override
    public void goBack() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (webView.canGoBack()) {
            WebBackForwardList list = webView.copyBackForwardList();
            int idx = list.getCurrentIndex();
            if (idx > 0) {
                String historyUrl = list.getItemAtIndex(idx - 1).getUrl();
                if (UrlProcessor.isCrossDomain(webView.getUrl(), historyUrl)
                        && sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").apply();
                }
                String profile = sp.getString("profile", "profileStandard");
                if (listStandard != null && listStandard.isWhite(historyUrl)) {
                    profile = HelperUnit.domain(historyUrl);
                }
                applyPreferences(sp, profile, historyUrl);
            }
        }
        webView.goBack();
    }

    @Override
    public void goForward() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (webView.canGoForward()) {
            WebBackForwardList list = webView.copyBackForwardList();
            int idx = list.getCurrentIndex();
            if (idx >= 0 && idx + 1 < list.getSize()) {
                String historyUrl = list.getItemAtIndex(idx + 1).getUrl();
                if (UrlProcessor.isCrossDomain(webView.getUrl(), historyUrl)
                        && sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").apply();
                }
                String profile = sp.getString("profile", "profileStandard");
                if (listStandard != null && listStandard.isWhite(historyUrl)) {
                    profile = HelperUnit.domain(historyUrl);
                }
                applyPreferences(sp, profile, historyUrl);
            }
        }
        webView.goForward();
    }

    @Override
    public boolean canGoBack() {
        return webView.canGoBack();
    }

    @Override
    public boolean canGoForward() {
        return webView.canGoForward();
    }

    @Override
    public void stopLoading() {
        stopped = true;
        loading = false;
        webView.stopLoading();
    }

    // ── BrowserEngine: State ─────────────────────────────────────────────

    @Override
    public String getUrl() {
        return webView.getUrl();
    }

    @Override
    public String getTitle() {
        return webView.getTitle();
    }

    @Override
    public Bitmap getFavicon() {
        return webView.getFavicon();
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    // ── BrowserEngine: Settings & Incognito ───────────────────────────────

    public static final String INCOGNITO_PROFILE_NAME = "incognito_profile";

    @Override
    public void setIncognito(boolean incognito) {
        this.isIncognito = incognito;
        WebSettings ws = webView.getSettings();
        if (incognito) {
            ws.setSaveFormData(false);
            ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
            ws.setGeolocationEnabled(false);
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                try {
                    ProfileStore store = ProfileStore.getInstance();
                    store.getOrCreateProfile(INCOGNITO_PROFILE_NAME);
                    WebViewCompat.setProfile(webView, INCOGNITO_PROFILE_NAME);
                } catch (Exception e) {
                    Log.w("WebViewEngine", "Failed to assign incognito profile", e);
                }
            }
        } else {
            ws.setSaveFormData(true);
            ws.setCacheMode(WebSettings.LOAD_DEFAULT);
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                try {
                    WebViewCompat.setProfile(webView, Profile.DEFAULT_PROFILE_NAME);
                } catch (Exception e) {
                    Log.w("WebViewEngine", "Failed to assign default profile", e);
                }
            }
        }
    }

    @Override
    public boolean isIncognito() {
        return isIncognito;
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    public void applyPreferences(SharedPreferences sp, String profile, String url) {
        String effectiveProfile = profile;
        if (listStandard != null && listStandard.isWhite(url)) {
            effectiveProfile = HelperUnit.domain(url);
        }
        this.adBlockEnabled = sp.getBoolean(effectiveProfile + "_adBlock", true);

        WebSettings ws = webView.getSettings();
        try {
            int fontSize = Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100")));
            ws.setTextZoom(fontSize);
        } catch (Exception e) {
            ws.setTextZoom(100);
        }

        // Dark mode
        boolean nightMode = sp.getBoolean(effectiveProfile + "_night", true);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(ws, nightMode);
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(ws, nightMode
                    ? WebSettingsCompat.FORCE_DARK_ON
                    : WebSettingsCompat.FORCE_DARK_OFF);
        }

        // User agent
        String desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
        String mobileUA = getCachedDefaultUserAgent();
        String ownUA = sp.getString("sp_userAgent", "");
        if (!ownUA.isEmpty() && sp.getBoolean("userAgentSwitch", false)) {
            mobileUA = ownUA;
        }
        if (sp.getBoolean(effectiveProfile + "_desktop", false)) {
            ws.setUserAgentString(desktopUA);
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(true);
            webView.setInitialScale(100);
        } else {
            ws.setUserAgentString(mobileUA);
            ws.setUseWideViewPort(false);
            ws.setLoadWithOverviewMode(false);
            webView.setInitialScale(0);
        }

        ws.setDomStorageEnabled(sp.getBoolean(effectiveProfile + "_dom", true));
        ws.setMediaPlaybackRequiresUserGesture(sp.getBoolean(effectiveProfile + "_saveData", false));
        ws.setBlockNetworkImage(!sp.getBoolean(effectiveProfile + "_images", true));
        ws.setGeolocationEnabled(!isIncognito && sp.getBoolean(effectiveProfile + "_location", false));
        ws.setJavaScriptEnabled(sp.getBoolean(effectiveProfile + "_javascript", true));
        ws.setJavaScriptCanOpenWindowsAutomatically(
                sp.getBoolean(effectiveProfile + "_javascriptPopUp", false));

        if (isIncognito) {
            ws.setSaveFormData(false);
            ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
            ws.setGeolocationEnabled(false);
        }

        // Cookies
        try {
            CookieManager mgr;
            if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                Profile profileObj = ProfileStore.getInstance().getOrCreateProfile(INCOGNITO_PROFILE_NAME);
                mgr = profileObj.getCookieManager();
            } else {
                mgr = CookieManager.getInstance();
            }
            boolean acceptCookies = !isIncognito && sp.getBoolean(effectiveProfile + "_cookies", true);
            mgr.setAcceptCookie(acceptCookies);
            mgr.setAcceptThirdPartyCookies(
                    webView, !isIncognito && acceptCookies && sp.getBoolean(effectiveProfile + "_cookiesThirdParty", false));
        } catch (Exception ignored) {
        }
    }

    private String getCachedDefaultUserAgent() {
        if (sCachedDefaultUserAgent == null) {
            try {
                sCachedDefaultUserAgent = WebSettings.getDefaultUserAgent(context);
            } catch (Exception e) {
                sCachedDefaultUserAgent = "";
            }
        }
        return sCachedDefaultUserAgent;
    }

    // ── BrowserEngine: Page Scrolling & Finding ───────────────────────────

    @Override
    public boolean pageUp(boolean top) {
        return webView.pageUp(top);
    }

    @Override
    public boolean pageDown(boolean bottom) {
        return webView.pageDown(bottom);
    }

    @Override
    public void findAllAsync(String find) {
        webView.findAllAsync(find);
    }

    @Override
    public void findNext(boolean forward) {
        webView.findNext(forward);
    }

    @Override
    public void clearMatches() {
        webView.clearMatches();
    }

    @Override
    public void clearHistory() {
        webView.clearHistory();
    }

    @Override
    public void clearCache(boolean includeDiskFiles) {
        webView.clearCache(includeDiskFiles);
    }

    @Nullable
    @Override
    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName) {
        return webView.createPrintDocumentAdapter(documentName);
    }

    // ── BrowserEngine: JS ─────────────────────────────────────────────────

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        webView.evaluateJavascript(script, callback);
    }

    // ── BrowserEngine: Lifecycle ──────────────────────────────────────────

    @Override
    public void onPause() {
        webView.onPause();
    }

    @Override
    public void onResume() {
        webView.onResume();
    }

    @Override
    public void destroy() {
        stopLoading();
        client = null;
        if (isIncognito) {
            try {
                webView.clearCache(true);
                webView.clearFormData();
                webView.clearSslPreferences();
                if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                    Profile incognitoProfile = ProfileStore.getInstance().getProfile(INCOGNITO_PROFILE_NAME);
                    if (incognitoProfile != null) {
                        incognitoProfile.getCookieManager().removeAllCookies(null);
                        incognitoProfile.getWebStorage().deleteAllData();
                    }
                }
            } catch (Exception ignored) {}
        }
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setDownloadListener(null);
        webView.onPause();
        webView.clearHistory();
        webView.setVisibility(View.GONE);
        if (webView.getParent() instanceof ViewGroup) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }
        webView.removeAllViews();
        webView.destroy();
    }

    @Override
    public void activate() {
        webView.requestFocus();
    }

    @Override
    public void deactivate() {
        webView.clearFocus();
    }

    // ── BrowserEngine: Client ─────────────────────────────────────────────

    @Override
    public void setClient(BrowserEngineClient client) {
        this.client = client;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private Map<String, String> getRequestHeaders(SharedPreferences sp, String profile, String targetUrl) {
        Map<String, String> headers = new HashMap<>();
        headers.put("DNT", "1");
        headers.put("Sec-GPC", "1");
        headers.put("X-Requested-With", "com.duckduckgo.mobile.android");
        String currentUrl = webView.getUrl();
        if (currentUrl != null && (currentUrl.startsWith("http://") || currentUrl.startsWith("https://"))) {
            // Prevent referer leakage to plain HTTP when navigating from HTTPS
            if (!(currentUrl.startsWith("https://") && targetUrl != null && targetUrl.startsWith("http://"))) {
                headers.put("Referer", currentUrl);
            }
        }
        if (sp.getBoolean(profile + "_saveData", false)) {
            headers.put("Save-Data", "on");
        }
        return headers;
    }

    // ── WebViewClient ─────────────────────────────────────────────────────

    private WebViewClient buildWebViewClient() {
        return new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loading = true;
                stopped = false;
                if (client != null) client.onPageStarted(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loading = false;
                if (client != null) client.onPageFinished(url, view.getTitle());
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                loading = false;
                if (client != null) {
                    client.onReceivedError(errorCode, description, failingUrl);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    loading = false;
                    if (client != null && error != null) {
                        client.onReceivedError(error.getErrorCode(),
                                error.getDescription() != null ? error.getDescription().toString() : "Error",
                                request.getUrl() != null ? request.getUrl().toString() : "");
                    }
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view,
                    android.webkit.RenderProcessGoneDetail detail) {
                loading = false;
                if (client != null) {
                    client.onReceivedError(-1, "Render process gone", view.getUrl());
                }
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler,
                    SslError error) {
                if (client != null) {
                    client.onSslError(error.getUrl(), new BrowserEngineClient.SslErrorHandler() {
                        @Override public void proceed() { handler.proceed(); }
                        @Override public void cancel()  { handler.cancel();  }
                    });
                } else {
                    handler.cancel();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (client != null) return client.shouldOverrideUrlLoading(url);
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                    WebResourceRequest request) {
                if (request == null || request.getUrl() == null) {
                    return super.shouldInterceptRequest(view, request);
                }
                String url = request.getUrl().toString();
                if (url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:") || url.startsWith("file:")) {
                    return super.shouldInterceptRequest(view, request);
                }
                if (adBlockEnabled && adBlock != null && adBlock.isAd(url)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(EMPTY_BYTES));
                }
                if (client != null && client.shouldBlockRequest(url)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(EMPTY_BYTES));
                }
                return super.shouldInterceptRequest(view, request);
            }
        };
    }

    // ── WebChromeClient ───────────────────────────────────────────────────

    private WebChromeClient buildWebChromeClient() {
        return new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (client != null) client.onProgressChanged(newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (client != null) client.onTitleChanged(title, view.getUrl());
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                if (client != null) client.onFaviconReceived(icon, view.getUrl());
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if (client != null) {
                    client.onJsAlert(message, result::confirm);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                if (client != null) {
                    client.onJsConfirm(message, result::confirm, result::cancel);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message,
                    String defaultValue, JsPromptResult result) {
                if (client != null) {
                    client.onJsPrompt(message, defaultValue, new BrowserEngineClient.PromptResult() {
                        @Override public void accept(String value) { result.confirm(value); }
                        @Override public void cancel()            { result.cancel();        }
                    });
                    return true;
                }
                return false;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                if (client != null) {
                    client.onPermissionRequest(BrowserEngineClient.PERMISSION_GEOLOCATION, origin,
                            () -> callback.invoke(origin, true, false),
                            () -> callback.invoke(origin, false, false));
                } else {
                    callback.invoke(origin, false, false);
                }
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                // No-op
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (client != null && request != null && request.getResources() != null) {
                    String[] resources = request.getResources();
                    for (String res : resources) {
                        String mapped = res;
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res)) {
                            mapped = BrowserEngineClient.PERMISSION_CAMERA;
                        } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res)) {
                            mapped = BrowserEngineClient.PERMISSION_MICROPHONE;
                        }
                        client.onPermissionRequest(mapped,
                                request.getOrigin() != null ? request.getOrigin().toString() : "",
                                () -> {
                                    try {
                                        request.grant(new String[]{res});
                                    } catch (Exception ignored) {}
                                },
                                () -> {
                                    try {
                                        request.deny();
                                    } catch (Exception ignored) {}
                                });
                    }
                } else if (request != null) {
                    request.deny();
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                // No-op
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (client != null) client.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                if (client != null) client.onHideCustomView();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (client != null) {
                    return client.onShowFileChooser(filePathCallback);
                }
                return false;
            }
        };
    }

    // ── DownloadListener ──────────────────────────────────────────────────

    private DownloadListener buildDownloadListener() {
        return (url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (client != null) {
                client.onDownloadRequested(url, contentDisposition, mimeType, contentLength);
            }
        };
    }
}
