# Threat Model & Security Architecture

This document defines the threat model, trust boundaries, protected assets, and mitigation strategies for FOSS Browser.

---

## 1. Scope & System Overview

FOSS Browser is an Android web browser built around a pluggable engine architecture:
- **Android System WebView:** Default engine utilizing Android's WebKit/Chromium-based runtime.
- **Servo Web Engine:** Embeddable, parallel browser engine written in Rust running via OpenGL ES.
- **Android Host Shell:** Java-based browser UI, tab controller, SQLite persistence, and preference management.

---

## 2. Protected Assets

| Asset | Sensitivity | Protection Goal |
|---|---|---|
| **Browsing History & Bookmarks** | High | Confidentiality and integrity; isolated from web content and third-party apps. |
| **Authentication Cookies & Session State** | High | Prevent cross-origin leakage and ensure incognito state is strictly separated from standard tabs. |
| **Hardware Resources (Camera, Mic, GPS)** | High | Never accessible without explicit, per-origin user consent. |
| **Local App Storage & Databases** | Critical | Protected against path traversal (Zip Slip), arbitrary file writes, and unauthorized SQL execution. |
| **User Navigation Flow** | Medium | Protected against malicious intent redirection, tracking scripts, and spoofed adblock hosts. |

---

## 3. Threat Actors & Attack Vectors

### 3.1. Malicious Web Origins (Untrusted Remote Content)
- **Threat:** A visited website attempts to execute arbitrary code, steal cookies, exploit DOM vulnerabilities, bypass Same-Origin Policy (SOP), or silently access hardware sensors.
- **Mitigations:**
  - Strict SOP, CORS, and CSP enforcement.
  - Universal and file-scheme access from web scripts is disabled (`setAllowUniversalAccessFromFileURLs(false)`).
  - Mixed content is blocked (`MIXED_CONTENT_NEVER_ALLOW`).
  - Web permissions require interactive dialog confirmation with the exact requesting origin shown.
  - Dynamic JavaScript injections use safe JSON quoting (`JSONObject.quote`) to prevent syntax breakout.

### 3.2. Network / Man-in-the-Middle (MITM) Attackers
- **Threat:** An attacker on a public Wi-Fi or compromised router intercepts network traffic, injects malicious scripts into HTTP responses, alters filter lists, or presents spoofed SSL certificates.
- **Mitigations:**
  - SSL errors are blocked by default with a critical security warning; primary action is connection termination.
  - Network Security Config restricts certificate trust anchors to system CAs.
  - AdBlock filter downloads strictly enforce HTTPS (`isHostUrlSafe`).
  - Active mixed content is disallowed.

### 3.3. Malicious Android Apps (Intent Spoofing & Inter-App Attacks)
- **Threat:** A malicious third-party app sends crafted intents to launch internal `file://`, `content://`, or `javascript:` URLs or hijack browsing state.
- **Mitigations:**
  - `shouldOverrideUrlLoading` blocks dangerous schemes (`file:`, `content:`, `javascript:`).
  - External intents are validated with `CATEGORY_BROWSABLE` and have component selectors cleared before launching.
  - `FileProvider` authorities are non-exported (`exported="false"`) with explicit grant permissions.

### 3.4. Malicious Backup Archives (Zip Slip / Path Traversal)
- **Threat:** An attacker crafts a zip file with relative path sequences (`../../`) to overwrite sensitive databases or shared preferences when imported.
- **Mitigations:**
  - `BackupUtils.isEntrySafe` validates all zip entries before extraction, rejecting absolute paths, drive letters, and entries outside the canonical app directory.

---

## 4. Trust Boundaries & Isolation

```
┌─────────────────────────────────────────────────────────────┐
│                       Internet                              │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS (TLS 1.2 / 1.3)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               Network Security Configuration                │
│             (System CA Trust Anchors Only)                  │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌───────────────────────────┐         ┌───────────────────────────┐
│     Standard Profile      │         │     Incognito Profile     │
│   (Default WebStorage,    │         │ (Multi-Profile Isolated,  │
│    Persistent Cookies)    │         │  In-Memory Storage Only)  │
└───────────┬───────────────┘         └───────────┬───────────────┘
            │                                     │
            └──────────────────┬──────────────────┘
                               │ BrowserEngine Interface
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Pluggable Rendering Engines                 │
│   ┌───────────────────────────┐ ┌─────────────────────────┐ │
│   │ Android System WebView    │ │ Servo Web Engine (Rust) │ │
│   │ (SOP, CORS, CSP Enforced) │ │ (Memory-Safe Sandboxed) │ │
│   └───────────────────────────┘ └─────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────┘
                               │ Per-Origin Permission Prompt
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Android Shell                          │
│     (Parameterized SQLite, Zip Slip Defenses, FLAG_SECURE)  │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Security Invariants

1. **Incognito Zero-Trace:** No browsing history, cache, or form data from incognito tabs is ever written to persistent storage.
2. **Explicit Consent:** No website can access the camera, microphone, or geolocation without an interactive prompt displaying the site's origin.
3. **No Unencrypted Tampering:** Security-critical configuration (such as AdBlock host lists) must only be retrieved over verified HTTPS connections.
4. **Isolated File Access:** Web content cannot access the local Android filesystem via `file://` or `content://` protocols.
