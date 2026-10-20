package de.baumann.browser.browser.engine;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;

/**
 * Abstract interface for browser rendering engines.
 *
 * <p>Implementations: {@link WebViewEngine} (Android WebView) and
 * {@link ServoEngine} (Servo browser engine).</p>
 *
 * <p>Callers interact exclusively through this interface so that
 * the host activity and {@link de.baumann.browser.view.NinjaWebView}
 * remain engine-agnostic.</p>
 */
public interface BrowserEngine {

    // ── View ─────────────────────────────────────────────────────────────

    /**
     * Returns the engine's rendering view (WebView, SurfaceView, etc.).
     * The caller is responsible for adding this to the layout hierarchy.
     */
    View getView();

    // ── Navigation ───────────────────────────────────────────────────────

    /**
     * Loads the given URL. Implementations should strip tracking params,
     * apply preferences, and handle the http→https upgrade dialog via
     * {@link UrlProcessor}.
     */
    void loadUrl(String url);

    /** Reloads the current page, re-applying current preferences. */
    void reload();

    /** Navigates back. No-op if {@link #canGoBack()} returns false. */
    void goBack();

    /** Navigates forward. No-op if {@link #canGoForward()} returns false. */
    void goForward();

    /** @return true if the engine has a previous history entry. */
    boolean canGoBack();

    /** @return true if the engine has a next history entry. */
    boolean canGoForward();

    /** Stops any active page load. */
    void stopLoading();

    // ── State ────────────────────────────────────────────────────────────

    /** @return the current page URL, or null if no page is loaded. */
    String getUrl();

    /** @return the current page title, or null. */
    String getTitle();

    /** @return the current page favicon, or null. */
    Bitmap getFavicon();

    /** @return true if a page load is currently in progress. */
    boolean isLoading();

    // ── Settings ─────────────────────────────────────────────────────────

    /**
     * Applies all SharedPreferences-backed settings to the engine.
     * Called before every navigation and on reload.
     *
     * @param sp      the app's default SharedPreferences
     * @param profile the active profile key (e.g. "profileStandard")
     * @param url     the URL about to be loaded (used for whitelist lookups)
     */
    void applyPreferences(SharedPreferences sp, String profile, String url);

    // ── JS Injection ─────────────────────────────────────────────────────

    /**
     * Evaluates a JavaScript string in the current page context.
     * Results are delivered asynchronously via the provided callback, or
     * the callback may be null.
     */
    void evaluateJavascript(String script, android.webkit.ValueCallback<String> callback);

    // ── Lifecycle ────────────────────────────────────────────────────────

    /** Called when the host activity/fragment pauses. Pause rendering. */
    void onPause();

    /** Called when the host activity/fragment resumes. Resume rendering. */
    void onResume();

    /**
     * Destroys the engine instance. After this call the engine must not
     * be used.
     */
    void destroy();

    // ── Tab / Focus ──────────────────────────────────────────────────────

    /**
     * Signals that this engine is now the foreground (active) tab.
     * Implementations should request focus and resume any paused rendering.
     */
    void activate();

    /**
     * Signals that this engine is now a background tab.
     * Implementations may throttle rendering to conserve resources.
     */
    void deactivate();

    // ── Client callback ──────────────────────────────────────────────────

    /**
     * Sets the callback client that receives page-lifecycle events.
     * Must be called before {@link #loadUrl}.
     */
    void setClient(BrowserEngineClient client);
}
