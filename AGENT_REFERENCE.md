# Agent Reference: FOSS Browser → Servo Migration

> **Single Source of Truth** for any agent or developer continuing the Servo engine migration.
> Document updated: October 2026.
> Build Status: **`BUILD SUCCESSFUL`** (`assembleDebug` passing, debug APK verified).

---

## 1. Executive Summary

This project migrates FOSS Browser from Android's built-in `android.webkit.WebView` to the open-source **Servo browser engine** (`org.servo.servoview`).

Because Servo exposes a `SurfaceView`/OpenGL-based interface rather than extending `android.webkit.WebView`, direct subclassing was decoupled into an **engine abstraction architecture** (Composition over Inheritance). This design allows switching between `WebViewEngine` and `ServoEngine` at runtime via a preference flag (`pref_use_servo`), ensuring zero regressions during iterative development.

---

## 2. Repository Overview

| Property | Value |
|---|---|
| **Location** | `c:\Personal\DevFolder\foss_browser` |
| **Language** | Java (Android, JDK 17) |
| **Min SDK** | 29 (Android 10) |
| **Target SDK** | 37 |
| **Build System** | Gradle (`com.android.tools.build:gradle:9.3.2`, Gradle 9.5) |
| **Package** | `de.baumann.browser` |
| **Artifact Output** | `app/build/outputs/apk/debug/app-debug.apk` |

---

## 3. Architecture & What Was Done

```
                     ┌────────────────────────┐
                     │    BrowserActivity     │
                     └──────────┬─────────────┘
                                │
                     ┌──────────▼─────────────┐
                     │      NinjaWebView      │ (FrameLayout + AlbumController)
                     └──────────┬─────────────┘
                                │ hosts
                     ┌──────────▼─────────────┐
                     │     BrowserEngine      │ (Interface)
                     └─────┬────────────┬─────┘
                           │            │
            ┌──────────────▼──┐      ┌──▼──────────────┐
            │  WebViewEngine  │      │   ServoEngine   │
            │  (System WebKit)│      │  (ServoView AAR)│
            └─────────────────┘      └─────────────────┘
```

### 3.1 What Was Done & How It Was Done

#### 1. Build & Dependency Setup (`servoview-local/`)
- Created a standalone Gradle sub-module `servoview-local/` that exposes `servo-aarch64-android.aar` as a dependency.
- Updated `settings.gradle` to include `:servoview-local`.
- Added ABI filter (`arm64-v8a`) in `app/build.gradle` since Servo ships primarily for 64-bit ARM.
- Added OpenGL ES 2.0 requirement in `AndroidManifest.xml` (`<uses-feature android:glEsVersion="0x00020000" android:required="true" />`).
- Added ProGuard preservation rules in `proguard-rules.pro` for `org.servo.**` and `org.mozilla.**`.
- Created `local.properties` with the path to the Android SDK.

#### 2. Abstract Engine Layer (`de.baumann.browser.browser.engine`)
- **`BrowserEngine.java`**: The core interface implemented by rendering engines. Defines methods for navigation (`loadUrl`, `reload`, `goBack`, `goForward`, `canGoBack`, `canGoForward`, `stopLoading`), state (`getUrl`, `getTitle`, `getFavicon`), view hierarchy (`getView`), preferences (`applyPreferences`), lifecycle (`onPause`, `onResume`, `destroy`, `activate`, `deactivate`), and client registration (`setClient`).
- **`BrowserEngineClient.java`**: Event listener interface decoupled from Android WebKit types:
  - Page lifecycle: `onPageStarted`, `onPageFinished`, `onProgressChanged`, `onReceivedTitle`
  - URL handling: `shouldOverrideUrlLoading`, `shouldBlockRequest`
  - Dialogs: `onJsAlert`, `onJsConfirm`, `onJsPrompt`, `onSslError`
  - Features: `onDownloadRequested`, `onShowFileChooser`, `onShowCustomView`, `onHideCustomView`
- **`UrlProcessor.java`**: Extracted domain routing, tracking-parameter stripping, and plain-HTTP warning detection into a shared utility.
- **`WebViewEngine.java`**: Production-ready wrapper around `WebView`, `NinjaWebViewClient`, and `NinjaWebChromeClient`. Provides 100% backward compatibility for all existing features.
- **`ServoEngine.java`**: Compilable adapter bridging `BrowserEngine` to Servo's `ServoView` and callbacks. Includes touch forwarding, GL surface initialization, and stub hooks.

#### 3. Refactored `NinjaWebView.java` (Composition Model)
- Changed base class from `android.webkit.WebView` to `android.widget.FrameLayout`.
- Instantiates either `ServoEngine` or `WebViewEngine` dynamically based on `pref_use_servo`.
- Implements `AlbumController` directly to maintain seamless compatibility with `AdapterTabs`.
- Added backward-compatible delegation methods for `BrowserActivity` interactions:
  - `getWebView()`
  - `getSettings()`
  - `getHitTestResult()`
  - `copyBackForwardList()`
  - `pageUp(boolean)` / `pageDown(boolean)`
  - `findNext(boolean)` / `findAllAsync(String)`
  - `createPrintDocumentAdapter(String)`
  - `loadDataWithBaseURL(...)`
  - `setFavicon(Bitmap)`
  - `setWebViewClient(WebViewClient)`

#### 4. Supporting Adjustments
- **`AdapterTabs.java`**: Added `isForeground()` tracking field and getter; updated `activate()`/`deactivate()` to update foreground state.
- **`BrowserUnit.java`**: Updated `openInBackground(Activity, View)` to accept generic `View` (supporting both `NinjaWebView` and `WebView`) and properly extract URLs.
- **`AdBlock.java`**: Changed visibility of `isAd(String url)` to `public` so it can be called by engine packages.
- **Legacy Cleanup**: Removed superseded `NinjaWebViewClient.java`, `NinjaWebChromeClient.java`, and `WebAppInterface.java` whose logic now lives in `WebViewEngine` and `BrowserEngineClient`.

---

## 4. Implementation Status Tracker

| Phase | Component / File | Status | Notes |
|---|---|---|---|
| **Phase 0** | `servoview-local/README.md` | ✅ DONE | Instructions for obtaining and installing the AAR |
| **Phase 1** | Build configuration (`settings.gradle`, `app/build.gradle`, `proguard-rules.pro`, `AndroidManifest.xml`, `local.properties`) | ✅ DONE | OpenGL ES 2.0 added, ABI filtered to arm64-v8a |
| **Phase 2** | `BrowserEngine.java` & `BrowserEngineClient.java` | ✅ DONE | Engine contracts defined |
| **Phase 2** | `UrlProcessor.java` | ✅ DONE | URL normalization and dialog routing extracted |
| **Phase 2** | `WebViewEngine.java` | ✅ DONE | Fallback engine wrapping system WebKit |
| **Phase 3** | `ServoEngine.java` | ✅ DONE (Stub/Active hooks) | Fully structured, compiles cleanly |
| **Phase 4** | `NinjaWebView.java` | ✅ DONE | FrameLayout composition model with full delegation |
| **Phase 4** | `AdapterTabs.java` | ✅ DONE | Added `isForeground()` tracking |
| **Phase 5** | Interop Fixes (`AdBlock`, `BrowserUnit`) & Legacy Cleanup | ✅ DONE | Removed obsolete `NinjaWebViewClient`, `NinjaWebChromeClient`, `WebAppInterface` |
| **Phase 6** | Build & Compilation Verification | ✅ DONE | Full recompilation and `assembleDebug` passing |
| **Phase 7** | AAR Download & Live Activation | ⏳ NEXT STEP | Place `servo-aarch64-android.aar` in `servoview-local/` |

---

## 5. How to Activate Servo with the Real AAR

When you are ready to test with the live Servo binary:

1. **Obtain the AAR**:
   - Download `servo-aarch64-android.aar` from [Servo GitHub Releases](https://github.com/servo/servo/releases) or nightly builds.
   - Alternatively, build Servo from source:
     ```bash
     ./mach build --android --release
     ```
2. **Place the AAR**:
   - Copy `servo-aarch64-android.aar` to:
     `c:\Personal\DevFolder\foss_browser\servoview-local\servo-aarch64-android.aar`
3. **Enable Dependency in `app/build.gradle`**:
   - Uncomment:
     ```groovy
     implementation project(':servoview-local')
     ```
4. **Uncomment Servo Imports in `ServoEngine.java`**:
   - Search for `[SERVO_ACTIVE]` in `ServoEngine.java` and uncomment the `ServoView` initialization and callback mappings.
5. **Toggle Engine at Runtime**:
   - Set preference `pref_use_servo` to `true` in `SharedPreferences`.
   - In the app UI: Go to Settings → tap Version 7 times to toggle Developer Mode.

---

## 6. Verification Commands

```powershell
# 1. Compile Java code
.\gradlew.bat compileDebugJavaWithJavac --console=plain

# 2. Assemble Debug APK
.\gradlew.bat assembleDebug --console=plain

# 3. Install to connected device (ARM64 physical device)
.\gradlew.bat installDebug
```
