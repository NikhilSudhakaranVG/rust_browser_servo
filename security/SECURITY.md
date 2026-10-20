# Security Policy

FOSS Browser takes security and user privacy seriously. This document outlines our security policies, how to report security vulnerabilities, supported versions, and our defense-in-depth architecture.

---

## 1. Supported Versions

Security updates and patches are actively maintained for the latest release and the primary branch.

| Version | Supported | Notes |
|---|---|---|
| `main` (trunk) | :white_check_mark: | Primary development branch |
| `v24` (Current release) | :white_check_mark: | Active production release |
| `< v24` | :x: | Deprecated; please upgrade to the latest release |

---

## 2. Reporting a Vulnerability

If you discover a security vulnerability or privacy flaw in FOSS Browser, please report it privately to allow for coordinated disclosure before public release.

### How to Report

- **Email:** Submit a vulnerability report to the repository maintainer.
- **GitHub Security Advisory:** Submit a private vulnerability report via [GitHub Security Advisories](https://github.com/NikhilNetroxe/rust_browser_servo/security/advisories/new) if available.
- **Response Timeline:**
  - **Initial Acknowledgement:** Within 48 hours.
  - **Triage & Assessment:** Within 5 business days.
  - **Fix & Disclosure Coordination:** Fixes will be prepared on a private branch and released alongside a security advisory.

### What to Include in Your Report

To help us triage and resolve the issue quickly, please include:
1. Description of the vulnerability and its potential impact.
2. Step-by-step reproduction instructions or a minimal Proof of Concept (PoC).
3. Affected components, files, or configurations.
4. Device environment (Android version, device model, WebView version, engine mode: WebView vs Servo).

---

## 3. Browser Security Architecture & Controls

FOSS Browser employs a layered defense-in-depth approach across UI, storage, network, and rendering engine boundaries:

### 3.1. Engine Isolation & Same-Origin Policy (SOP)
- **Android System WebView:** Runs web rendering with isolated origin boundaries. Strict Same-Origin Policy (SOP), Cross-Origin Resource Sharing (CORS), and Content Security Policy (CSP) enforcement.
- **Universal & File URL Access Blocked:** `setAllowFileAccessFromFileURLs(false)` and `setAllowUniversalAccessFromFileURLs(false)` are strictly enforced to prevent local file exfiltration.
- **Mixed Content Enforcement:** `MIXED_CONTENT_NEVER_ALLOW` ensures no insecure cleartext scripts or resources load in HTTPS sessions.
- **Servo Web Engine Integration:** Parallel layout and rendering in Rust with memory safety guarantees against buffer overflows, use-after-free, and data races.

### 3.2. Incognito & Multi-Profile Storage Isolation
- **AndroidX Multi-Profile:** Employs `androidx.webkit.WebViewFeature.MULTI_PROFILE` to assign incognito tabs to a separate, isolated `"incognito_profile"`.
- **Zero Disk Leakage:** Incognito sessions use a dedicated, in-memory `CookieManager` and separate `WebStorage` (LocalStorage / IndexedDB).
- **Session Purging:** When incognito tabs close, session storage, cookies, and cache are purged.
- **Screen & Recents Protection:** `FLAG_SECURE` prevents Android task switcher snapshots and screen recording while incognito tabs are in focus.

### 3.3. Web Permissions & Hardware Access
- **Per-Origin Consent:** Camera, microphone, and geolocation requests require explicit user confirmation via dialog showing the exact origin.
- **Site Remembering:** Granular per-origin allow/deny rules stored deterministically (`web_perm_<origin>_<resource>`).
- **Profile Overrides:** Global profile switches can disallow hardware access entirely regardless of site requests.

### 3.4. Input Sanitization & Navigation Filtering
- **Zip Slip Mitigation:** Backup archive extraction in `BackupUtils` validates all zip entries against path traversal attacks (`../`, absolute paths, drive letters).
- **SQL Injection Defense:** All SQLite database interactions in `RecordAction` use parameterized queries.
- **Scheme Filtering:** Dangerous URL schemes (`file:`, `content:`, `javascript:`) and unverified intent URIs are blocked from arbitrary navigation.
- **DOM XSS Escaping:** HTML entities and JSON variables are safely escaped and quoted when dynamically injected into JavaScript contexts.
- **AdBlock HTTPS Integrity:** Filter host downloads strictly require HTTPS to prevent MITM tampering.

---

## 4. Security Documentation Hub

- [Six-Month Security Audit Report (2026)](AUDIT_REPORT_2026.md)
- [Architectural Remediation Plan](REMEDIATION_PLAN.md)
- [Threat Model & Risk Assessment](THREAT_MODEL.md)
