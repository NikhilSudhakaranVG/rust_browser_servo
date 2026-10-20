package de.baumann.browser.feature.browser.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintDocumentAdapter;
import android.util.Log;
import android.widget.FrameLayout;
import android.view.View;
import android.webkit.ValueCallback;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

/**
 * {@link BrowserEngine} implementation backed by the Servo browser engine.
 *
 * <p><b>SETUP REQUIRED:</b> This class will not compile until the Servo AAR is
 * placed in {@code servoview-local/} and the dependency is uncommented in
 * {@code app/build.gradle}. See {@code servoview-local/README.md} for instructions.</p>
 *
 * <h3>Architecture</h3>
 * <pre>
 * ServoEngine
 *   └── FrameLayout (container — returned by getView())
 *        └── ServoView (org.servo.servoview.ServoView)
 * </pre>
 *
 * <p>{@code ServoView} is a {@code SurfaceView}-based component. It must be
 * embedded inside a container {@code FrameLayout} so it can be resized and
 * swapped in the host layout without issues.</p>
 *
 * <h3>Threading</h3>
 * <p>Servo renders on a dedicated GL thread. All {@code ServoView} method
 * calls must happen on the <b>main thread</b>. Servo callbacks arrive on the
 * main thread as well.</p>
 *
 * <h3>Activation</h3>
 * <p>Background {@code ServoView} instances should be paused via
 * {@link #deactivate()} to release GPU resources.</p>
 */
public class ServoEngine implements BrowserEngine {

    private static final String TAG = "ServoEngine";

    // ── Fields ────────────────────────────────────────────────────────────

    private final Context context;
    private final FrameLayout container;
    private final UrlProcessor urlProcessor;
    private final Handler mainHandler;
    private Runnable pendingStubRunnable = null;

    private BrowserEngineClient client;

    // [SERVO_ACTIVE] Uncomment when AAR is available:
    // private org.servo.servoview.ServoView servoView;

    private String currentUrl  = null;
    private String currentTitle = null;
    private boolean loading = false;
    private boolean active  = false;
    private boolean isIncognito = false;

    // ── Constructor ───────────────────────────────────────────────────────

    public ServoEngine(Context context) {
        this.context      = context;
        this.container    = new FrameLayout(context);
        this.urlProcessor = new UrlProcessor(context);
        this.mainHandler  = new Handler(Looper.getMainLooper());

        // [SERVO_ACTIVE] Replace the following STUB block with:
        //
        //   Servo.init(context, context.getAssets());
        //   servoView = new org.servo.servoview.ServoView(context, null);
        //   servoView.setClient(buildServoClient());
        //   container.addView(servoView,
        //       new FrameLayout.LayoutParams(
        //           FrameLayout.LayoutParams.MATCH_PARENT,
        //           FrameLayout.LayoutParams.MATCH_PARENT));

        // ── STUB: placeholder view shown until AAR is present ─────────────
        android.widget.TextView stub = new android.widget.TextView(context);
        stub.setText("⚙ Servo engine not yet linked.\n"
                + "Place servo-aarch64-android.aar in servoview-local/\n"
                + "and uncomment the dependency in app/build.gradle.");
        stub.setGravity(android.view.Gravity.CENTER);
        stub.setPadding(32, 32, 32, 32);
        container.addView(stub, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        // ── END STUB ──────────────────────────────────────────────────────

        Log.i(TAG, "ServoEngine created (STUB mode — AAR not yet linked)");
    }

    // ── BrowserEngine: View ───────────────────────────────────────────────

    @Override
    public View getView() {
        return container;
    }

    // ── BrowserEngine: Navigation ─────────────────────────────────────────

    @Override
    public void loadUrl(String url) {
        loadUrl(url, false);
    }

    @Override
    public void loadUrl(String url, boolean skipDialogs) {
        cancelPendingStub();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");

        UrlProcessor.Result result = urlProcessor.process(url, sp, profile, skipDialogs);

        if (!skipDialogs && (result.showTrackingDialog || result.showHttpDialog)) {
            if (result.showTrackingDialog && client != null) {
                loading = false;
                client.shouldOverrideUrlLoading("internal://tracking-dialog?url="
                        + Uri.encode(result.url)
                        + "&clean=" + Uri.encode(result.cleanUrl));
                return;
            }
            if (result.showHttpDialog && client != null) {
                loading = false;
                client.shouldOverrideUrlLoading("internal://http-dialog?url="
                        + Uri.encode(result.url));
                return;
            }
        }

        currentUrl = result.url;
        loading = true;
        Log.i(TAG, "loadUrl: " + result.url + " (STUB — no-op until AAR linked)");

        // [SERVO_ACTIVE]: servoView.loadUri(result.url);

        // STUB: fire synthetic start/finish callbacks so the UI doesn't hang
        if (client != null) {
            client.onPageStarted(result.url);
            pendingStubRunnable = () -> {
                pendingStubRunnable = null;
                loading = false;
                currentTitle = "Servo (STUB)";
                if (client != null) {
                    client.onPageFinished(result.url, currentTitle);
                    client.onProgressChanged(100);
                }
            };
            mainHandler.postDelayed(pendingStubRunnable, 300);
        }
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        loadUrl("data:" + (mimeType != null ? mimeType : "text/html")
                + ";charset=" + (encoding != null ? encoding : "UTF-8")
                + "," + Uri.encode(data), true);
    }

    @Override
    public void reload() {
        if (currentUrl != null) {
            loadUrl(currentUrl, true);
        }
        // [SERVO_ACTIVE]: if (servoView != null) servoView.reload();
    }

    @Override
    public void goBack() {
        // [SERVO_ACTIVE]: if (servoView != null) servoView.goBack();
        Log.i(TAG, "goBack (STUB)");
    }

    @Override
    public void goForward() {
        // [SERVO_ACTIVE]: if (servoView != null) servoView.goForward();
        Log.i(TAG, "goForward (STUB)");
    }

    @Override
    public boolean canGoBack() {
        // [SERVO_ACTIVE]: return servoView != null && servoView.canGoBack();
        return false;
    }

    @Override
    public boolean canGoForward() {
        // [SERVO_ACTIVE]: return servoView != null && servoView.canGoForward();
        return false;
    }

    @Override
    public void stopLoading() {
        cancelPendingStub();
        loading = false;
        // [SERVO_ACTIVE]: if (servoView != null) servoView.stop();
        Log.i(TAG, "stopLoading (STUB)");
    }

    // ── BrowserEngine: State ──────────────────────────────────────────────

    @Override public String  getUrl()     { return currentUrl;   }
    @Override public String  getTitle()   { return currentTitle; }
    @Override public Bitmap  getFavicon() { return null;         }
    @Override public boolean isLoading()  { return loading;      }

    // ── BrowserEngine: Settings & Incognito ───────────────────────────────

    @Override
    public void setIncognito(boolean incognito) {
        this.isIncognito = incognito;
        // [SERVO_ACTIVE]: map incognito/private mode to Servo configuration
    }

    @Override
    public boolean isIncognito() {
        return isIncognito;
    }

    @Override
    public void applyPreferences(SharedPreferences sp, String profile, String url) {
        // [SERVO_ACTIVE]: map each prefs key to the Servo preferences API.
        Log.d(TAG, "applyPreferences (STUB — preferences not yet applied to Servo)");
    }

    // ── BrowserEngine: Page Scrolling & Finding ───────────────────────────

    @Override
    public boolean pageUp(boolean top) {
        // [SERVO_ACTIVE]: delegate to Servo view scrolling
        return false;
    }

    @Override
    public boolean pageDown(boolean bottom) {
        // [SERVO_ACTIVE]: delegate to Servo view scrolling
        return false;
    }

    @Override
    public void findAllAsync(String find) {
        // [SERVO_ACTIVE]: delegate to Servo search
    }

    @Override
    public void findNext(boolean forward) {
        // [SERVO_ACTIVE]: delegate to Servo search next
    }

    @Override
    public void clearMatches() {
        // [SERVO_ACTIVE]: delegate to Servo clear matches
    }

    @Override
    public void clearHistory() {
        // [SERVO_ACTIVE]: delegate to Servo clear history
    }

    @Override
    public void clearCache(boolean includeDiskFiles) {
        // [SERVO_ACTIVE]: delegate to Servo clear cache
    }

    @Nullable
    @Override
    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName) {
        return null;
    }

    // ── BrowserEngine: JS ─────────────────────────────────────────────────

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        // [SERVO_ACTIVE]: servoView.evaluateJavascript(script);
        Log.i(TAG, "evaluateJavascript (STUB): " + script.substring(0, Math.min(40, script.length())));
        if (callback != null) callback.onReceiveValue(null);
    }

    // ── BrowserEngine: Lifecycle ───────────────────────────────────────────

    @Override
    public void onPause() {
        // [SERVO_ACTIVE]: if (servoView != null) servoView.onPause();
        Log.d(TAG, "onPause (STUB)");
    }

    @Override
    public void onResume() {
        // [SERVO_ACTIVE]: if (servoView != null) servoView.onResume();
        Log.d(TAG, "onResume (STUB)");
    }

    @Override
    public void destroy() {
        cancelPendingStub();
        client = null;
        container.removeAllViews();
        // [SERVO_ACTIVE]: if (servoView != null) { servoView.onPause(); servoView = null; }
        Log.i(TAG, "destroy (STUB)");
    }

    @Override
    public void activate() {
        active = true;
        // [SERVO_ACTIVE]: if (servoView != null) servoView.onResume();
    }

    @Override
    public void deactivate() {
        active = false;
        // [SERVO_ACTIVE]: if (servoView != null) servoView.onPause();
    }

    // ── BrowserEngine: Client ─────────────────────────────────────────────

    @Override
    public void setClient(BrowserEngineClient client) {
        this.client = client;
    }

    // ── Internal Helpers ──────────────────────────────────────────────────

    private void cancelPendingStub() {
        if (pendingStubRunnable != null) {
            mainHandler.removeCallbacks(pendingStubRunnable);
            pendingStubRunnable = null;
        }
    }

    // ── Servo callback builder ────────────────────────────────────────────

    // [SERVO_ACTIVE] Uncomment and adapt when AAR is linked:
    //
    // private org.servo.servoview.ServoView.Client buildServoClient() {
    //     return new org.servo.servoview.ServoView.Client() {
    //
    //         @Override public void onLoadStarted() {
    //             loading = true;
    //             currentUrl = servoView.getUrl();
    //             if (client != null) client.onPageStarted(currentUrl);
    //         }
    //
    //         @Override public void onLoadEnded() {
    //             loading = false;
    //             if (client != null) client.onPageFinished(currentUrl, currentTitle);
    //             if (client != null) client.onProgressChanged(100);
    //         }
    //
    //         @Override public void onTitleChanged(String title) {
    //             currentTitle = title;
    //             if (client != null) client.onTitleChanged(title, currentUrl);
    //         }
    //
    //         @Override public void onUrlChanged(String url) {
    //             currentUrl = url;
    //         }
    //
    //         @Override public void onProgressChanged(int progress) {
    //             if (client != null) client.onProgressChanged(progress);
    //         }
    //
    //         @Override public void showAlert(String message, org.servo.servoview.Servo.GeckoPrefCallback cb) {
    //             if (client != null) client.onJsAlert(message, cb::invoke);
    //         }
    //
    //         @Override public void showConfirm(String message, org.servo.servoview.Servo.GeckoPrefCallback cb) {
    //             if (client != null) client.onJsConfirm(message, () -> cb.invoke(true), () -> cb.invoke(false));
    //         }
    //
    //         @Override public void showPrompt(String message, String def, org.servo.servoview.Servo.StringCallback cb) {
    //             if (client != null) client.onJsPrompt(message, def,
    //                 new BrowserEngineClient.PromptResult() {
    //                     public void accept(String v) { cb.invoke(v); }
    //                     public void cancel()         { cb.invoke(null); }
    //                 });
    //         }
    //     };
    // }
}
