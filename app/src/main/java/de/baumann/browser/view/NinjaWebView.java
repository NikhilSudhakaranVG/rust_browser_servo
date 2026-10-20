package de.baumann.browser.view;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.print.PrintDocumentAdapter;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.browser.engine.BrowserEngine;
import de.baumann.browser.browser.engine.BrowserEngineClient;
import de.baumann.browser.browser.engine.ServoEngine;
import de.baumann.browser.browser.engine.UrlProcessor;
import de.baumann.browser.browser.engine.WebViewEngine;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

/**
 * Primary browser content view — now a {@link FrameLayout} that hosts a
 * {@link BrowserEngine} rather than extending {@link android.webkit.WebView}
 * directly.
 *
 * <p>This approach (composition over inheritance) allows the active rendering
 * engine ({@link WebViewEngine} or {@link ServoEngine}) to be swapped at
 * runtime without changing any of the surrounding UI code.</p>
 *
 * <h3>Engine selection</h3>
 * <p>The engine is chosen based on the {@code pref_use_servo} SharedPreference
 * (default {@code false}). It can be toggled in Settings → About by tapping
 * the version number 7 times.</p>
 *
 * <h3>Invariant</h3>
 * <p>All callers must use the public methods declared here. Direct access to
 * the underlying engine via {@link #getEngine()} should be avoided except for
 * operations not yet exposed by this wrapper.</p>
 */
public class NinjaWebView extends FrameLayout implements AlbumController {

    // ── Engine-selection preference key ───────────────────────────────────
    /** SharedPreference key — set to {@code true} to activate Servo. */
    public static final String PREF_USE_SERVO = "pref_use_servo";

    // ── Fields ─────────────────────────────────────────────────────────────
    public boolean fingerPrintProtection;
    public boolean history;
    public boolean adBlock;
    public boolean saveData;
    public boolean camera;

    private final Context context;
    private boolean stopped;
    private AdapterTabs album;
    private AlbumController predecessor = null;
    private static BrowserController browserController = null;
    private static SharedPreferences sp;

    private final BrowserEngine engine;
    private Bitmap favicon;

    // ── Constructors ──────────────────────────────────────────────────────

    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.engine = buildEngine(context);
        init();
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.engine = buildEngine(context);
        init();
    }

    public NinjaWebView(Context context) {
        super(context);
        this.context = context;
        this.engine = buildEngine(context);
        init();
    }

    // ── Factory ───────────────────────────────────────────────────────────

    private static BrowserEngine buildEngine(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useServo = prefs.getBoolean(PREF_USE_SERVO, false);
        if (useServo) {
            Log.i(TAG, "NinjaWebView: using ServoEngine");
            return new ServoEngine(context);
        } else {
            Log.i(TAG, "NinjaWebView: using WebViewEngine");
            return new WebViewEngine(context);
        }
    }

    // ── Initialisation ────────────────────────────────────────────────────

    private void init() {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");

        this.stopped = false;
        this.fingerPrintProtection = sp.getBoolean(profile + "_fingerPrintProtection", true);
        this.history               = sp.getBoolean(profile + "_history", true);
        this.adBlock               = sp.getBoolean(profile + "_adBlock", false);
        this.saveData              = sp.getBoolean(profile + "_saveData", false);
        this.camera                = sp.getBoolean(profile + "_camera", false);

        // Add the engine view to our layout
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(engine.getView(), lp);

        // Wire up the engine callback
        engine.setClient(buildEngineClient());

        // Initialise tab thumbnail adapter
        this.album = new AdapterTabs(context, this, browserController);
        album.setBrowserController(browserController);
    }

    // ── Engine client (BrowserEngineClient impl) ──────────────────────────

    private BrowserEngineClient buildEngineClient() {
        return new BrowserEngineClient() {

            @Override
            public void onPageStarted(String url) {
                stopped = false;
                if (isForeground() && browserController != null)
                    browserController.updateProgress(0);
            }

            @Override
            public void onPageFinished(String url, String title) {
                if (isForeground() && !stopped && browserController != null)
                    browserController.updateProgress(BrowserUnit.LOADING_STOPPED);
                album.setAlbumTitle(title, url);
                // Save history
                if (history && url != null && !url.equals("about:blank")) {
                    RecordAction action = new RecordAction(context);
                    action.open(true);
                    action.addHistory(new Record(title, url, 0, 0));
                    action.close();
                }
                // Save favicon
                Bitmap favicon = engine.getFavicon();
                if (favicon != null && url != null) {
                    FaviconHelper faviconHelper = new FaviconHelper(context);
                    faviconHelper.addFavicon(context, url, favicon);
                }
                updateFavicon(url);
            }

            @Override
            public void onProgressChanged(int progress) {
                if (isForeground() && !stopped && browserController != null)
                    browserController.updateProgress(progress);
            }

            @Override
            public void onTitleChanged(String title, String url) {
                album.setAlbumTitle(title, url);
            }

            @Override
            public void onFaviconReceived(Bitmap icon, String url) {
                updateFavicon(url);
            }

            @Override
            public void onReceivedError(int errorCode, String description, String url) {
                Log.e(TAG, "Engine error " + errorCode + " at " + url + ": " + description);
            }

            @Override
            public void onSslError(String url, BrowserEngineClient.SslErrorHandler handler) {
                // Show SSL error dialog
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.app_error)
                        .setMessage("SSL Certificate Error: " + url)
                        .setCancelable(false)
                        .setPositiveButton(R.string.app_ok, (d, w) -> handler.proceed())
                        .setNegativeButton(R.string.app_cancel, (d, w) -> handler.cancel())
                        .show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(String url) {
                // Handle internal dialog protocols injected by UrlProcessor
                if (url.startsWith("internal://tracking-dialog")) {
                    handleTrackingDialog(url);
                    return true;
                }
                if (url.startsWith("internal://http-dialog")) {
                    handleHttpDialog(url);
                    return true;
                }
                // External app handling (tel:, mailto:, etc.)
                if (!url.startsWith("http://") && !url.startsWith("https://")
                        && !url.startsWith("about:") && !url.startsWith("data:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot open URL: " + url);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldBlockRequest(String url) {
                return false; // Ad blocking is handled inside WebViewEngine directly
            }

            @Override
            public void onDownloadRequested(String url, String contentDisposition,
                    String mimeType, long contentLength) {
                // Delegate to existing NinjaDownloadListener logic via BrowserUnit
                BrowserUnit.download(context, url, contentDisposition, mimeType);
            }

            @Override
            public void onJsAlert(String message, Runnable confirm) {
                new MaterialAlertDialogBuilder(context)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w) -> confirm.run())
                        .show();
            }

            @Override
            public void onJsConfirm(String message, Runnable confirm, Runnable cancel) {
                new MaterialAlertDialogBuilder(context)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w) -> confirm.run())
                        .setNegativeButton(android.R.string.cancel, (d, w) -> cancel.run())
                        .show();
            }

            @Override
            public void onJsPrompt(String message, String defaultValue,
                    BrowserEngineClient.PromptResult result) {
                final TextInputEditText input = new TextInputEditText(context);
                input.setText(defaultValue);
                new MaterialAlertDialogBuilder(context)
                        .setMessage(message)
                        .setView(input)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok,
                                (d, w) -> result.accept(Objects.requireNonNull(
                                        input.getText()).toString()))
                        .setNegativeButton(android.R.string.cancel, (d, w) -> result.cancel())
                        .show();
            }

            @Override
            public void onPermissionRequest(String resource, String origin,
                    Runnable grant, Runnable deny) {
                // Auto-grant based on profile settings (camera, location, mic)
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String profile = prefs.getString("profile", "profileStandard");
                boolean shouldGrant = false;
                switch (resource) {
                    case "camera":       shouldGrant = prefs.getBoolean(profile + "_camera", false); break;
                    case "microphone":   shouldGrant = prefs.getBoolean(profile + "_microphone", false); break;
                    case "geolocation":  shouldGrant = prefs.getBoolean(profile + "_location", false); break;
                }
                if (shouldGrant) grant.run(); else deny.run();
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                if (browserController != null) browserController.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                if (browserController != null) browserController.onHideCustomView();
            }
        };
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Returns the active rendering engine. */
    public BrowserEngine getEngine() {
        return engine;
    }

    public boolean isForeground() {
        // album tracks foreground state via activate()/deactivate()
        return album != null && album.isForeground();
    }

    public static BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController controller) {
        browserController = controller;
        if (album != null) album.setBrowserController(controller);
    }

    // ── Navigation (delegated to engine) ─────────────────────────────────

    @SuppressLint("ApplySharedPref")
    public synchronized void loadUrl(@NonNull String url) {
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(getWindowToken(), 0);

        stopped = false;
        engine.loadUrl(url);
    }

    public synchronized void reload() {
        stopped = false;
        engine.reload();
    }

    public synchronized void goBack() {
        engine.goBack();
    }

    public void goForward() {
        engine.goForward();
    }

    public boolean canGoBack() {
        return engine.canGoBack();
    }

    public boolean canGoForward() {
        return engine.canGoForward();
    }

    public synchronized void stopLoading() {
        stopped = true;
        engine.stopLoading();
    }

    // ── State (delegated to engine) ───────────────────────────────────────

    public String getUrl()   { return engine.getUrl();   }
    public String getTitle() { return engine.getTitle(); }

    @Nullable
    public Bitmap getFavicon() {
        return favicon != null ? favicon : engine.getFavicon();
    }

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;
    }

    // ── Preferences (called by engine client on page start) ───────────────

    public synchronized void initPreferences(String url) {
        String profile = sp.getString("profile", "profileStandard");
        List_standard listStd = new List_standard(context);
        if (listStd.isWhite(url)) profile = HelperUnit.domain(url);

        fingerPrintProtection = sp.getBoolean(profile + "_fingerPrintProtection", false);
        history               = sp.getBoolean(profile + "_saveHistory", true);
        adBlock               = sp.getBoolean(profile + "_adBlock", true);
        saveData              = sp.getBoolean(profile + "_saveData", true);
        camera                = sp.getBoolean(profile + "_camera", true);

        engine.applyPreferences(sp, profile, url);
    }

    // ── Tab state ─────────────────────────────────────────────────────────

    public void setAlbumTitle(String title, String url) {
        album.setAlbumTitle(title, url);
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        album.activate();
        engine.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        album.deactivate();
        engine.deactivate();
    }

    // ── Title / favicon updates ───────────────────────────────────────────

    public synchronized void updateTitle(int progress) {
        if (isForeground() && !stopped && browserController != null)
            browserController.updateProgress(progress);
        else if (isForeground() && browserController != null)
            browserController.updateProgress(BrowserUnit.LOADING_STOPPED);
    }

    public synchronized void updateTitle(String title, String url) {
        album.setAlbumTitle(title, url);
    }

    public synchronized void updateFavicon(String url) {
        FaviconHelper.setFavicon(context, album.getAlbumView(), url,
                R.id.item_icon, R.drawable.icon_image_broken);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public synchronized void destroy() {
        stopLoading();
        engine.onPause();
        engine.destroy();
        removeAllViews();
    }

    public void onPause()  { engine.onPause();  }
    public void onResume() { engine.onResume(); }

    // ── Engine access & WebView compatibility methods ────────────────────

    public BrowserEngine getEngine() {
        return engine;
    }

    @Nullable
    public WebView getWebView() {
        if (engine instanceof WebViewEngine) {
            return ((WebViewEngine) engine).getWebView();
        }
        return null;
    }

    public void setWebViewClient(WebViewClient client) {
        WebView wv = getWebView();
        if (wv != null) {
            wv.setWebViewClient(client);
        }
    }

    @Nullable
    public WebSettings getSettings() {
        WebView wv = getWebView();
        return wv != null ? wv.getSettings() : null;
    }

    @Nullable
    public WebView.HitTestResult getHitTestResult() {
        WebView wv = getWebView();
        return wv != null ? wv.getHitTestResult() : null;
    }

    @Nullable
    public WebBackForwardList copyBackForwardList() {
        WebView wv = getWebView();
        return wv != null ? wv.copyBackForwardList() : null;
    }

    public boolean pageUp(boolean top) {
        WebView wv = getWebView();
        return wv != null && wv.pageUp(top);
    }

    public boolean pageDown(boolean bottom) {
        WebView wv = getWebView();
        return wv != null && wv.pageDown(bottom);
    }

    public void findNext(boolean forward) {
        WebView wv = getWebView();
        if (wv != null) {
            wv.findNext(forward);
        }
    }

    public void findAllAsync(String find) {
        WebView wv = getWebView();
        if (wv != null) {
            wv.findAllAsync(find);
        }
    }

    @Nullable
    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName) {
        WebView wv = getWebView();
        return wv != null ? wv.createPrintDocumentAdapter(documentName) : null;
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        WebView wv = getWebView();
        if (wv != null) {
            wv.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
        } else if (data != null) {
            engine.loadUrl("data:" + (mimeType != null ? mimeType : "text/html") + ";charset=" + (encoding != null ? encoding : "UTF-8") + "," + Uri.encode(data));
        }
    }

    // ── Profile helpers ───────────────────────────────────────────────────

    public void setProfileDefaultValues() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addBookmark(new Record("FOSS Browser - WIKI",
                "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki", 0, 0));
        action.close();

        sp.edit()
                .putBoolean("profileStandard_saveData", true)
                .putBoolean("profileStandard_images", true)
                .putBoolean("profileStandard_adBlock", true)
                .putBoolean("profileStandard_trackingULS", false)
                .putBoolean("profileStandard_location", false)
                .putBoolean("profileStandard_fingerPrintProtection", false)
                .putBoolean("profileStandard_cookies", true)
                .putBoolean("profileStandard_cookiesThirdParty", false)
                .putBoolean("profileStandard_deny_cookie_banners", true)
                .putBoolean("profileStandard_javascript", true)
                .putBoolean("profileStandard_javascriptPopUp", false)
                .putBoolean("profileStandard_saveHistory", true)
                .putBoolean("profileStandard_camera", false)
                .putBoolean("profileStandard_microphone", false)
                .putBoolean("profileStandard_dom", true)
                .putBoolean("profileStandard_night", true)
                .putBoolean("profileStandard_desktop", false).apply();
    }

    public void setProfileChanged() {
        sp.edit()
                .putString("profile", "profileChanged")
                .putBoolean("profileChanged_saveData",    sp.getBoolean("profileStandard_saveData", true))
                .putBoolean("profileChanged_images",      sp.getBoolean("profileStandard_images", true))
                .putBoolean("profileChanged_adBlock",     sp.getBoolean("profileStandard_adBlock", true))
                .putBoolean("profileChanged_trackingULS", sp.getBoolean("profileStandard_trackingULS", true))
                .putBoolean("profileChanged_location",    sp.getBoolean("profileStandard_location", false))
                .putBoolean("profileChanged_fingerPrintProtection",
                        sp.getBoolean("profileStandard_fingerPrintProtection", true))
                .putBoolean("profileChanged_cookies",
                        sp.getBoolean("_cookies", false))
                .putBoolean("profileChanged_cookiesThirdParty",
                        sp.getBoolean("profileStandard_cookiesThirdParty", false))
                .putBoolean("profileChanged_deny_cookie_banners",
                        sp.getBoolean("profileStandard_deny_cookie_banners", false))
                .putBoolean("profileChanged_javascript",
                        sp.getBoolean("profileStandard_javascript", true))
                .putBoolean("profileChanged_javascriptPopUp",
                        sp.getBoolean("profileStandard_javascriptPopUp", false))
                .putBoolean("profileChanged_saveHistory",
                        sp.getBoolean("profileStandard_saveHistory", true))
                .putBoolean("profileChanged_camera",
                        sp.getBoolean("profileStandard_camera", false))
                .putBoolean("profileChanged_microphone",
                        sp.getBoolean("profileStandard_microphone", false))
                .putBoolean("profileChanged_dom",    sp.getBoolean("profileStandard_dom", true))
                .putBoolean("profileChanged_night",  sp.getBoolean("profileStandard_night", true))
                .putBoolean("profileChanged_desktop",sp.getBoolean("profileStandard_desktop", false))
                .apply();
    }

    public static String getProfile() {
        return sp.getString("profile", "profileStandard");
    }

    // ── Predecessor (tab opening hierarchy) ───────────────────────────────

    public AlbumController getPredecessor()                        { return predecessor; }
    public void setPredecessor(AlbumController predecessor)        { this.predecessor = predecessor; }

    // ── Misc helpers ──────────────────────────────────────────────────────

    public void setStopped(boolean stopped) { this.stopped = stopped; }
    public boolean isAdBlock()              { return adBlock; }
    public boolean isHistory()              { return history; }
    public boolean isSaveData()             { return saveData; }
    public boolean isCamera()              { return camera; }
    public boolean isFingerPrintProtection(){ return fingerPrintProtection; }
    public void resetFavicon()              { /* favicon managed by engine */ }

    /** Evaluates JavaScript in the current page (delegates to engine). */
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        engine.evaluateJavascript(script, callback);
    }

    // ── Internal dialog handlers ──────────────────────────────────────────

    /**
     * Handles the "internal://tracking-dialog" synthetic URL that
     * {@link WebViewEngine} fires when tracking params are detected.
     */
    private void handleTrackingDialog(String syntheticUrl) {
        Uri parsed   = Uri.parse(syntheticUrl);
        String url   = parsed.getQueryParameter("url");
        String clean = parsed.getQueryParameter("clean");
        if (url == null) return;

        final String finalUrl   = url;
        final String finalClean = clean != null ? clean : url;

        GridItem item01 = new GridItem(context.getString(R.string.app_ok), R.drawable.icon_check);
        GridItem item02 = new GridItem(context.getString(R.string.app_no), R.drawable.icon_close);
        GridItem item03 = new GridItem(context.getString(R.string.menu_edit), R.drawable.icon_edit);

        View dialogView = View.inflate(context, R.layout.dialog_menu, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        String tracking = finalUrl.substring(finalUrl.lastIndexOf("?"));
        String msg = context.getString(R.string.dialog_tracking) + " \"" + tracking + "\"?";
        if (msg.length() > 150) msg = msg.substring(0, 150) + " [...]?\"";

        TextView overflowMessage = dialogView.findViewById(R.id.overflowMessage);
        overflowMessage.setText(msg);
        TextView overflowURL = dialogView.findViewById(R.id.overflowURL);
        overflowURL.setText(finalUrl);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Button cancel = dialogView.findViewById(R.id.menuCancel);
        cancel.setVisibility(View.VISIBLE);
        cancel.setOnClickListener(v -> { dialog.cancel(); loadUrl("about:blank"); });
        dialog.show();
        dialog.setCancelable(false);
        HelperUnit.setupDialog(context, dialog);

        GridView menuGrid = dialogView.findViewById(R.id.menu_grid);
        List<GridItem> gridList = new LinkedList<>();
        gridList.add(item01);
        gridList.add(item02);
        gridList.add(item03);
        GridAdapter adapter = new GridAdapter(context, gridList);
        menuGrid.setAdapter(adapter);
        menuGrid.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            switch (position) {
                case 0: engine.loadUrl(BrowserUnit.queryWrapper(context, finalClean)); break;
                case 1: engine.loadUrl(BrowserUnit.queryWrapper(context, finalUrl));   break;
                case 2:
                    View editView = View.inflate(context, R.layout.dialog_edit_text, null);
                    TextInputEditText input = editView.findViewById(R.id.textInput);
                    input.setText(finalUrl);
                    HelperUnit.showSoftKeyboard(input);
                    new MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.menu_edit)
                            .setIcon(R.drawable.icon_tracking)
                            .setView(editView)
                            .setPositiveButton(R.string.app_ok, (d, i) -> {
                                d.dismiss();
                                engine.loadUrl(BrowserUnit.queryWrapper(context,
                                        Objects.requireNonNull(input.getText()).toString()));
                            })
                            .create().show();
                    break;
            }
        });
    }

    /**
     * Handles the "internal://http-dialog" synthetic URL that
     * {@link WebViewEngine} fires for plain-HTTP URLs.
     */
    private void handleHttpDialog(String syntheticUrl) {
        Uri parsed = Uri.parse(syntheticUrl);
        String url = parsed.getQueryParameter("url");
        if (url == null) return;

        String secure   = url.replace("http://", "https://");
        String unSecure = url;

        GridItem item01 = new GridItem("https://", R.drawable.icon_secure);
        GridItem item02 = new GridItem("http://",  R.drawable.icon_unsecure);
        GridItem item03 = new GridItem(context.getString(R.string.app_never), R.drawable.icon_close);

        View dialogView = View.inflate(context, R.layout.dialog_menu, null);
        CardView card = dialogView.findViewById(R.id.item_CardViewItem);
        card.setVisibility(View.GONE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(HelperUnit.domain(url))
                .setIcon(R.drawable.icon_unsecure)
                .setMessage(R.string.toast_unsecured)
                .setView(dialogView)
                .create();

        Button cancel = dialogView.findViewById(R.id.menuCancel);
        cancel.setVisibility(View.VISIBLE);
        cancel.setOnClickListener(v -> { dialog.cancel(); loadUrl("about:blank"); });
        dialog.show();
        dialog.setCancelable(false);
        HelperUnit.setupDialog(context, dialog);

        GridView menuGrid = dialogView.findViewById(R.id.menu_grid);
        List<GridItem> gridList = new LinkedList<>();
        gridList.add(item01);
        gridList.add(item02);
        gridList.add(item03);
        GridAdapter adapter = new GridAdapter(context, gridList);
        menuGrid.setAdapter(adapter);
        menuGrid.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            switch (position) {
                case 0:
                    engine.loadUrl(BrowserUnit.queryWrapper(context, secure));
                    break;
                case 1:
                    engine.loadUrl(BrowserUnit.queryWrapper(context, unSecure));
                    break;
                case 2:
                    sp.edit().putString("dialog_neverAsk", "yes").apply();
                    engine.loadUrl(BrowserUnit.queryWrapper(context, unSecure));
                    break;
            }
        });
    }
}