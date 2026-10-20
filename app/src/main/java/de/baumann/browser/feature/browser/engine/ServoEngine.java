package de.baumann.browser.browser.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.FrameLayout;
import android.view.View;
import android.webkit.ValueCallback;

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
 *
 * <!-- ═══════════════════════════════════════════════════════════════════
 *   HOW TO ACTIVATE THIS CLASS
 *   ─────────────────────────────────────────────────────────────────────
 *   1. Download servo-aarch64-android.aar from:
 *        https://github.com/servo/servo/releases
 *   2. Place it at: servoview-local/servo-aarch64-android.aar
 *   3. In app/build.gradle, uncomment:
 *        implementation project(':servoview-local')
 *   4. In this file, uncomment every line marked // [SERVO_ACTIVE]
 *   5. Delete the STUB section below this comment block.
 * ═══════════════════════════════════════════════════════════════════ -->
 */
public class ServoEngine implements BrowserEngine {

    private static final String TAG = "ServoEngine";

    // ── Fields ────────────────────────────────────────────────────────────

    private final Context context;
    private final FrameLayout container;
    private BrowserEngineClient client;

    // [SERVO_ACTIVE] Uncomment when AAR is available:
    // private org.servo.servoview.ServoView servoView;

    private String currentUrl  = null;
    private String currentTitle = null;
    private boolean loading = false;
    private boolean active  = false;

    // ── Constructor ───────────────────────────────────────────────────────

    public ServoEngine(Context context) {
        this.context   = context;
        this.container = new FrameLayout(context);

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
        currentUrl = url;
        loading = true;
        Log.i(TAG, "loadUrl: " + url + " (STUB — no-op until AAR linked)");

        // [SERVO_ACTIVE]: servoView.loadUri(url);

        // STUB: fire synthetic start/finish callbacks so the UI doesn't hang
        if (client != null) {
            client.onPageStarted(url);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                loading = false;
                currentTitle = "Servo (STUB)";
                if (client != null) client.onPageFinished(url, currentTitle);
                if (client != null) client.onProgressChanged(100);
            }, 300);
        }
    }

    @Override
    public void reload() {
        if (currentUrl != null) {
            loadUrl(currentUrl);
        }
        // [SERVO_ACTIVE]: servoView.reload();
    }

    @Override
    public void goBack() {
        // [SERVO_ACTIVE]: servoView.goBack();
        Log.i(TAG, "goBack (STUB)");
    }

    @Override
    public void goForward() {
        // [SERVO_ACTIVE]: servoView.goForward();
        Log.i(TAG, "goForward (STUB)");
    }

    @Override
    public boolean canGoBack() {
        // [SERVO_ACTIVE]: return servoView.canGoBack();
        return false;
    }

    @Override
    public boolean canGoForward() {
        // [SERVO_ACTIVE]: return servoView.canGoForward();
        return false;
    }

    @Override
    public void stopLoading() {
        loading = false;
        // [SERVO_ACTIVE]: servoView.stop();
        Log.i(TAG, "stopLoading (STUB)");
    }

    // ── BrowserEngine: State ──────────────────────────────────────────────

    @Override public String  getUrl()     { return currentUrl;   }
    @Override public String  getTitle()   { return currentTitle; }
    @Override public Bitmap  getFavicon() { return null;         }
    @Override public boolean isLoading()  { return loading;      }

    // ── BrowserEngine: Settings ───────────────────────────────────────────

    @Override
    public void applyPreferences(SharedPreferences sp, String profile, String url) {
        // TODO (Phase 5): map each prefs key to the Servo preferences API.
        //
        // When the AAR is available, reference the Servo preferences like:
        //   servoView.setJavaScriptEnabled(sp.getBoolean(profile+"_javascript", true));
        //   servoView.setUserAgent(userAgentString);
        //   // etc.
        //
        // Until then, preferences are silently ignored in STUB mode.
        Log.d(TAG, "applyPreferences (STUB — preferences not yet applied to Servo)");
    }

    // ── BrowserEngine: JS ─────────────────────────────────────────────────

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        // [SERVO_ACTIVE]: servoView.evaluateJavascript(script);
        //   (Servo's JS result delivery mechanism differs — adapt callback when known.)
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
        container.removeAllViews();
        // [SERVO_ACTIVE]: if (servoView != null) { servoView.onPause(); servoView = null; }
        Log.i(TAG, "destroy (STUB)");
    }

    @Override
    public void activate() {
        active = true;
        // [SERVO_ACTIVE]: servoView.onResume();
    }

    @Override
    public void deactivate() {
        active = false;
        // [SERVO_ACTIVE]: servoView.onPause();  // free GPU resources in background tabs
    }

    // ── BrowserEngine: Client ─────────────────────────────────────────────

    @Override
    public void setClient(BrowserEngineClient client) {
        this.client = client;
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
    //
    //         // Add further Servo.Client callbacks here as the API evolves.
    //     };
    // }
}
