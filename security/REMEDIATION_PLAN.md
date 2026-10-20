# Comprehensive Remediation Plan — Browser Security Architecture

This plan addresses the architectural risks and browser security controls identified in the **Six-Month Security Audit**, building upon the core code fixes patched and verified in git commits [`6182ed0`](file:///c:/Personal/DevFolder/foss_browser) and [`dcd036b`](file:///c:/Personal/DevFolder/foss_browser).

---

## 1. Objectives & Scope

| ID | Finding / Area | Target Component | Proposed Action | Status |
|---|---|---|---|---|
| **P1** | **Per-Origin Web Permissions** (SEC-09) | `NinjaWebView.java` | Replace silent profile auto-granting with an interactive, per-origin confirmation dialog (`Allow`, `Deny`, `Remember`). | **Completed** ([`dcd036b`](file:///c:/Personal/DevFolder/foss_browser)) |
| **P2** | **Network Security Config & Cleartext Scoping** (SEC-10) | `AndroidManifest.xml`, `AdBlock.java` | Add `network_security_config.xml` to restrict cleartext traffic to user-navigated web sessions, enforce HTTPS on internal host file downloads, and disable unencrypted fallback. | **Completed** ([`dcd036b`](file:///c:/Personal/DevFolder/foss_browser)) |
| **P3** | **Incognito Storage & Cookie Isolation** (SEC-11) | `WebViewEngine.java`, `NinjaWebView.java` | Implement `androidx.webkit.WebViewFeature.MULTI_PROFILE` support to assign an isolated, non-persistent profile to incognito tabs, preventing cookie and `localStorage` leakage with standard tabs. | **Completed** ([`dcd036b`](file:///c:/Personal/DevFolder/foss_browser)) |
| **P4** | **Servo Engine Ephemeral Isolation Hook** | `ServoEngine.java` | Structure the pending Servo engine JNI callbacks to configure ephemeral in-memory storage when `setIncognito(true)` is activated. | **Completed** ([`dcd036b`](file:///c:/Personal/DevFolder/foss_browser)) |
| **P5** | **Test Suite Expansion** | `SecurityRegressionTest.java` | Expand unit tests to validate scheme blocking, permission origin mapping, and host verification. | **Completed** ([`dcd036b`](file:///c:/Personal/DevFolder/foss_browser)) |

---

## 2. Implementation Details

### Phase 1: Interactive Per-Origin Web Permission Prompts (`NinjaWebView.java`)
1. **Permission Store:**
   - Saved origin decisions are keyed deterministically via `web_perm_<origin>_<resource>`.
2. **Interactive `onPermissionRequest` in `NinjaWebView.java`:**
   - First verifies that the global profile switch (`profile_camera`, `profile_microphone`, `profile_location`) is enabled. If disabled, denies immediately.
   - If enabled in profile:
     - Checks stored decision for the origin. If previously remembered, applies `grant.run()` or `deny.run()` without prompting.
     - If no stored decision exists: displays a `MaterialAlertDialogBuilder` dialog showing:
       - **Title:** Application title
       - **Message:** `"<origin> requests access to your [camera/microphone/location]. Do you want to grant access?"`
       - **Options:** `Allow`, `Block`, and a `"Remember for this site"` checkbox.
3. **Cancellation & Thread Safety:**
   - Managed with `AtomicBoolean` to guarantee `grant.run()` or `deny.run()` is invoked at most once. Cancelling or dismissing the dialog automatically denies to prevent hanging native permission queues.

---

### Phase 2: Network Security Config & HTTPS Enforcement
1. **Network Security Configuration:**
   - Path: `app/src/main/res/common/xml/network_security_config.xml`
   - Anchors connections to system CAs and regulates cleartext traffic.
   - Linked in `AndroidManifest.xml` via `android:networkSecurityConfig="@xml/network_security_config"`.
2. **AdBlock Hosts HTTPS Validation:**
   - In `AdBlock.java`: Enforces `isHostUrlSafe(hostUrl)`. Disallows plain `http://`, `ftp://`, or invalid URLs to ensure ad-blocking filter lists cannot be tampered with in transit.

---

### Phase 3: True Storage & Cookie Isolation for Incognito Mode
1. **AndroidX WebKit Multi-Profile Integration:**
   - In `WebViewEngine.java`, checks `WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)`.
   - When supported, assigns incognito tabs to `"incognito_profile"` via `WebViewCompat.setProfile(webView, "incognito_profile")`.
   - Configures the incognito profile's dedicated `CookieManager` without affecting standard tabs.
2. **Storage Purging on Tab Closure:**
   - Upon incognito tab destruction, `CookieManager.removeAllCookies()` and `WebStorage.deleteAllData()` wipe incognito session state.
3. **Fallback for Older WebViews:**
   - Maintains `setSaveFormData(false)`, `LOAD_NO_CACHE`, and manual cache/cookie flushing when Multi-Profile is unsupported on older system WebViews.

---

### Phase 4: Servo Engine Ephemeral Isolation Hook
1. **Private Mode Architecture in `ServoEngine.java`:**
   - Structured `setIncognito(boolean)` to set ephemeral configuration flags ready for the Servo AAR runtime.
   - In `destroy()`, prepared ephemeral storage invalidation hook for in-memory cookies and cache.

---

### Phase 5: Verification & Test Strategy
1. **Security Regression Tests:**
   - Expanded `SecurityRegressionTest.java` with test cases:
     - `testAdBlockHttpsEnforcement`
     - `testDangerousSchemeFiltering`
     - `testPermissionOriginKeyGeneration`
     - `testZipSlipPathTraversalBlocked`
     - `testHtmlEscapingPreventsTagInjection`
     - `testZipSlipExactTargetMatching`
2. **Build Validation:**
   - All tests pass via `.\gradlew.bat testDebugUnitTest`.
   - Full application builds successfully via `.\gradlew.bat assembleDebug`.
