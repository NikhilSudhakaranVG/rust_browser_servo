package de.baumann.browser.browser.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

/**
 * {@link BrowserEngine} implementation backed by Android's {@link WebView}.
 *
 * <p>This adapter wraps the existing WebView + WebViewClient + WebChromeClient
 * stack so that the host can interact with it through the engine-agnostic
 * {@link BrowserEngine} interface. The goal is <em>zero behaviour change</em>
 * relative to the original {@link de.baumann.browser.view.NinjaWebView}
 * implementation.</p>
 *
 * <p>All WebView-specific logic lives here. The host never imports
 * {@code android.webkit} directly.</p>
 */
public class WebViewEngine implements BrowserEngine {

    // ── Fields ────────────────────────────────────────────────────────────

    private final Context context;
    private final WebView webView;
    private BrowserEngineClient client;
    private AdBlock adBlock;
    private List_standard listStandard;
    private boolean loading = false;
    private boolean stopped = false;
    private final UrlProcessor urlProcessor;

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
        settings.setJavaScriptEnabled(true); // will be properly set in applyPreferences()
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(true);
        settings.setAllowFileAccess(true);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setSafeBrowsingEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

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

    // ── BrowserEngine: Navigation ─────────────────────────────────────────

    @Override
    public void loadUrl(String url) {
        stopped = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");
        listStandard = new List_standard(context);
        if (listStandard.isWhite(url)) profile = HelperUnit.domain(url);

        // Profile reset on domain change
        if (UrlProcessor.isCrossDomain(webView.getUrl(), url)
                && sp.getBoolean("sp_standard_always", true)) {
            sp.edit().putString("profile", "profileStandard").apply();
            profile = "profileStandard";
        }

        UrlProcessor.Result result = urlProcessor.process(url, sp, profile);

        if (result.showTrackingDialog || result.showHttpDialog) {
            // Delegate dialog display back to the client (BrowserActivity handles UI)
            if (result.showTrackingDialog && client != null) {
                // Fire a synthetic "override" so the host can show the dialog
                // The host calls loadUrl again with the user's chosen URL.
                stopped = true;
                webView.stopLoading();
                client.shouldOverrideUrlLoading("internal://tracking-dialog?url="
                        + Uri.encode(result.url)
                        + "&clean=" + Uri.encode(result.cleanUrl));
                return;
            }
            if (result.showHttpDialog && client != null) {
                stopped = true;
                webView.stopLoading();
                client.shouldOverrideUrlLoading("internal://http-dialog?url="
                        + Uri.encode(result.url));
                return;
            }
        }

        applyPreferences(sp, profile, result.url);
        webView.loadUrl(result.url, getRequestHeaders(sp, profile));
    }

    @Override
    public void reload() {
        stopped = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");
        applyPreferences(sp, profile, webView.getUrl());
        webView.reload();
    }

    @Override
    public void goBack() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        // Reset profile on domain change when going back
        if (webView.canGoBack()) {
            android.webkit.WebBackForwardList list = webView.copyBackForwardList();
            int idx = list.getCurrentIndex();
            if (idx > 0) {
                String historyUrl = list.getItemAtIndex(idx - 1).getUrl();
                if (UrlProcessor.isCrossDomain(webView.getUrl(), historyUrl)
                        && sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").commit();
                }
                applyPreferences(sp, sp.getString("profile", "profileStandard"), historyUrl);
            }
        }
        webView.goBack();
    }

    @Override
    public void goForward() {
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

    // ── BrowserEngine: Settings ───────────────────────────────────────────

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    public void applyPreferences(SharedPreferences sp, String profile, String url) {
        listStandard = new List_standard(context);
        String effectiveProfile = profile;
        if (listStandard.isWhite(url)) {
            effectiveProfile = HelperUnit.domain(url);
        }

        WebSettings ws = webView.getSettings();
        ws.setTextZoom(Integer.parseInt(
                Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));

        // Dark mode
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                    ws, sp.getBoolean(effectiveProfile + "_night", true));
        }

        // User agent
        String desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
        String mobileUA = WebSettings.getDefaultUserAgent(context);
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
        ws.setMediaPlaybackRequiresUserGesture(sp.getBoolean(effectiveProfile + "_saveData", true));
        ws.setBlockNetworkImage(!sp.getBoolean(effectiveProfile + "_images", true));
        ws.setGeolocationEnabled(sp.getBoolean(effectiveProfile + "_location", false));
        ws.setJavaScriptEnabled(sp.getBoolean(effectiveProfile + "_javascript", true));
        ws.setJavaScriptCanOpenWindowsAutomatically(
                sp.getBoolean(effectiveProfile + "_javascriptPopUp", false));

        // Cookies
        try {
            CookieManager mgr = CookieManager.getInstance();
            mgr.setAcceptCookie(sp.getBoolean(effectiveProfile + "_cookies", true));
            mgr.setAcceptThirdPartyCookies(
                    webView, sp.getBoolean(effectiveProfile + "_cookiesThirdParty", false));
        } catch (Exception ignored) {
        }
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
        webView.pauseTimers();
    }

    @Override
    public void onResume() {
        webView.resumeTimers();
        webView.onResume();
    }

    @Override
    public void destroy() {
        stopLoading();
        webView.onPause();
        webView.clearHistory();
        webView.setVisibility(View.GONE);
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

    private java.util.HashMap<String, String> getRequestHeaders(SharedPreferences sp, String profile) {
        java.util.HashMap<String, String> headers = new java.util.HashMap<>();
        headers.put("DNT", "1");
        headers.put("Sec-GPC", "1");
        headers.put("X-Requested-With", "com.duckduckgo.mobile.android");
        headers.put("Referer", webView.getUrl() != null ? webView.getUrl() : "");
        if (sp.getBoolean(profile + "_saveData", true)) headers.put("Save-Data", "on");
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
            public boolean onRenderProcessGone(WebView view,
                    android.webkit.RenderProcessGoneDetail detail) {
                // Notify client with a generic error
                if (client != null) {
                    client.onReceivedError(-1, "Render process gone", view.getUrl());
                }
                return true; // handled — do not crash the app
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
                String url = request.getUrl().toString();
                if (client != null) return client.shouldOverrideUrlLoading(url);
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                    WebResourceRequest request) {
                String url = request.getUrl().toString();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                String profile = sp.getString("profile", "profileStandard");
                boolean adBlockEnabled = sp.getBoolean(profile + "_adBlock", true);
                if (adBlockEnabled && adBlock.isAd(url)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream("".getBytes()));
                }
                if (client != null && client.shouldBlockRequest(url)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream("".getBytes()));
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
                    client.onPermissionRequest("geolocation", origin,
                            () -> callback.invoke(origin, true, false),
                            () -> callback.invoke(origin, false, false));
                } else {
                    callback.invoke(origin, false, false);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (client != null) {
                    String resource = request.getResources().length > 0
                            ? request.getResources()[0] : "unknown";
                    client.onPermissionRequest(resource, request.getOrigin().toString(),
                            () -> request.grant(request.getResources()),
                            request::deny);
                } else {
                    request.deny();
                }
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
                // Delegate to the activity via BrowserController
                // (unchanged — BrowserActivity already handles this)
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
