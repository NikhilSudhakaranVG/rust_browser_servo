package de.baumann.browser.feature.browser.engine;
import de.baumann.browser.feature.browser.BrowserActivity;
import de.baumann.browser.feature.browser.NinjaWebView;

import android.graphics.Bitmap;

/**
 * Callback interface from a {@link BrowserEngine} back to its host.
 *
 * <p>The host (typically {@link de.baumann.browser.feature.browser.NinjaWebView} or
 * {@link de.baumann.browser.feature.browser.BrowserActivity}) implements this
 * interface to receive asynchronous page-lifecycle events from whichever
 * engine is active.</p>
 */
public interface BrowserEngineClient {

    // ── Navigation events ─────────────────────────────────────────────────

    /**
     * Fired when a new page load begins.
     *
     * @param url the URL being loaded
     */
    void onPageStarted(String url);

    /**
     * Fired when the current page has finished loading.
     *
     * @param url   the final URL (may differ from the request URL due to redirects)
     * @param title the page title at load completion
     */
    void onPageFinished(String url, String title);

    /**
     * Fired periodically during loading.
     *
     * @param progress 0–100 inclusive; 100 means complete
     */
    void onProgressChanged(int progress);

    /**
     * Fired when the page title changes (may fire multiple times per load).
     *
     * @param title the new title
     * @param url   the URL whose title changed
     */
    void onTitleChanged(String title, String url);

    /**
     * Fired when the favicon for the current page is available.
     *
     * @param icon the favicon bitmap
     * @param url  the page URL
     */
    void onFaviconReceived(Bitmap icon, String url);

    // ── Error / security events ───────────────────────────────────────────

    /**
     * Fired when a page-level load error occurs.
     *
     * @param errorCode   engine-specific error code
     * @param description human-readable description
     * @param url         the URL that failed
     */
    void onReceivedError(int errorCode, String description, String url);

    /**
     * Fired on an SSL certificate error.
     *
     * @param url     the URL with the error
     * @param handler call {@code handler.proceed()} or {@code handler.cancel()}
     */
    void onSslError(String url, SslErrorHandler handler);

    // ── URL interception ─────────────────────────────────────────────────

    /**
     * Called before a URL is loaded. Return {@code true} to consume the
     * navigation (e.g. open in external app, handle custom scheme).
     *
     * @param url the URL about to be loaded
     * @return true if the client has handled the navigation
     */
    boolean shouldOverrideUrlLoading(String url);

    /**
     * Called for each sub-resource request (images, scripts, etc.).
     * Return {@code true} to block the request (ad-blocking use-case).
     *
     * @param url the resource URL
     * @return true to block the request
     */
    boolean shouldBlockRequest(String url);

    // ── Download ─────────────────────────────────────────────────────────

    /**
     * Fired when the engine detects a downloadable resource.
     *
     * @param url                the download URL
     * @param contentDisposition the Content-Disposition header value
     * @param mimeType           the detected MIME type
     * @param contentLength      the expected file size in bytes, or -1
     */
    void onDownloadRequested(String url, String contentDisposition,
                             String mimeType, long contentLength);

    // ── JS dialogs ───────────────────────────────────────────────────────

    /**
     * Fired for a JS {@code alert()} call.
     *
     * @param message the alert message
     * @param confirm call to dismiss the alert
     */
    void onJsAlert(String message, Runnable confirm);

    /**
     * Fired for a JS {@code confirm()} call.
     *
     * @param message the confirmation message
     * @param confirm call when the user presses OK
     * @param cancel  call when the user presses Cancel
     */
    void onJsConfirm(String message, Runnable confirm, Runnable cancel);

    /**
     * Fired for a JS {@code prompt()} call.
     *
     * @param message      the prompt message
     * @param defaultValue pre-filled value
     * @param result       call {@code result.accept(value)} with user input
     */
    void onJsPrompt(String message, String defaultValue, PromptResult result);

    // ── Permissions ───────────────────────────────────────────────────────

    /**
     * Fired when the page requests camera/microphone/geolocation access.
     *
     * @param resource  the permission being requested (e.g. "camera")
     * @param origin    the requesting origin
     * @param grant     call to grant the permission
     * @param deny      call to deny the permission
     */
    void onPermissionRequest(String resource, String origin, Runnable grant, Runnable deny);

    // ── Fullscreen video ─────────────────────────────────────────────────

    /**
     * Fired when a page element enters fullscreen (e.g. HTML5 video).
     *
     * @param view     the view to display fullscreen
     * @param callback call {@code onCustomViewHidden()} to exit fullscreen
     */
    void onShowCustomView(android.view.View view,
                          android.webkit.WebChromeClient.CustomViewCallback callback);

    /** Fired when fullscreen mode ends. */
    void onHideCustomView();

    // ── Helper types ─────────────────────────────────────────────────────

    /** Handler for SSL errors (mirrors {@code android.webkit.SslErrorHandler}). */
    interface SslErrorHandler {
        /** Proceed despite the SSL error. */
        void proceed();
        /** Cancel the failing request. */
        void cancel();
    }

    /** Result handle for JS prompt dialogs. */
    interface PromptResult {
        /** Accept the prompt with the given user-entered value. */
        void accept(String value);
        /** Cancel the prompt. */
        void cancel();
    }
}
