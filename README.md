<p align="center">
<img src="fastlane/metadata/android/en-US/images/featuresGraphic.png" alt="FOSS Browser" width="600"/>
</p>

__[README](README.md)__ | __[WIKI](wiki/)__ | __[Security](security/SECURITY.md)__ | __[FAQs](FAQs.md)__ | __[Changelog](CHANGELOG.md)__ | __[Privacy](PRIVACY.md)__ | __[License](LICENSE.md)__ | __[Code of conduct](CODE_OF_CONDUCT.md)__

----

FOSS Browser is a privacy-focused, open-source Android web browser optimized for one-handed use. It features a modern, pluggable engine architecture supporting both the standard Android System WebView and experimental integration with the next-generation, Rust-powered [Servo browser engine](https://servo.org/).

----

### Privacy

FOSS Browser uses profiles to protect your privacy. For each domain or standard profile, you can enable or disable: AdBlock, JavaScript, cookies, fingerprint protection, and much more. Saved domain settings always overwrite the standard profile, allowing trusted domains (e.g. `example.com` ⇒ custom settings) to open with specific permissions while remaining protected on unknown sites.

_More privacy features:_

- **Built-in AdBlock**: Automatically updated filter list blocking unwanted network requests (AdBlock hosts from [Steven Black](https://github.com/StevenBlack/hosts)) — [`AdBlock.java`](app/src/main/java/de/baumann/browser/feature/browser/AdBlock.java).
- **Third-party cookies**: Block third-party trackers with independent cookie policy control — [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java).
- **Cookie banner blocker**: Automatically block intrusive consent banners — [`BannerBlock.java`](app/src/main/java/de/baumann/browser/feature/browser/BannerBlock.java).
- **Tracking URL stripping**: Detects and strips tracking query parameters before navigation — [`UrlProcessor.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/UrlProcessor.java).
- **Fingerprint protection**: Defend against browser and hardware fingerprinting (including WebRTC IP leaks) — [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java).
- **Auto-wipe on exit**: Automatically clear cache, cookies, and history upon closing the app — [`DeleteUtils.java`](app/src/main/java/de/baumann/browser/feature/settings/DeleteUtils.java), [`Settings_Clear.java`](app/src/main/java/de/baumann/browser/feature/settings/Settings_Clear.java).
- **Android Autofill**: Enable or disable system autofill integration — [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java).
- **Custom search engines & redirects**: Choose between privacy-respecting search providers or configure custom redirect rules — [`CustomSearchesHelper.java`](app/src/main/java/de/baumann/browser/feature/settings/CustomSearchesHelper.java), [`CustomRedirectsHelper.java`](app/src/main/java/de/baumann/browser/feature/settings/CustomRedirectsHelper.java).
- **Zero data collection**: FOSS Browser does not collect or transmit any telemetry ([Privacy policy](PRIVACY.md)).

----

### Incognito (Private Browsing)

True private browsing mode with zero footprint and active screen protection:

- **Isolated tab state**: Tab-level incognito flag suppresses history persistence and form autofill — [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java), [`AlbumController.java`](app/src/main/java/de/baumann/browser/feature/browser/AlbumController.java).
- **Screen & recents protection**: Automatically applies `FLAG_SECURE` while an incognito tab is active to block screenshots and Android task switcher previews — [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java).
- **No session persistence**: Skips saving open incognito tabs to storage, ensuring they are never reloaded on app restart — [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java).
- **Data purging & session cookie flush**: Clears web cache, form data, SSL preferences upon closing incognito tabs, and flushes session cookies when the last incognito tab is closed — [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java), [`BrowserContainer.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserContainer.java).
- **Private Home View & Badging**: Dedicated incognito home view hiding normal bookmarks and distinct card styling — [`HomePageView.java`](app/src/main/java/de/baumann/browser/feature/browser/HomePageView.java), [`AdapterTabs.java`](app/src/main/java/de/baumann/browser/feature/browser/AdapterTabs.java), [`layout_home_page.xml`](app/src/main/res/main/layout/layout_home_page.xml), [`icon_incognito.xml`](app/src/main/res/common/drawable/icon_incognito.xml).
- **Quick access**: Open via incognito FAB in tab overview ([`dialog_overview.xml`](app/src/main/res/main/layout/dialog_overview.xml)), long-press on new-tab buttons, overflow menu, link long-press, or gesture action `33`.

----

### UI & Handling

FOSS Browser is built with Material You design libraries. Following system day/night mode and a wallpaper-based dynamic theme are just two features of this interface. The UI is purposefully optimized for one-handed use:

- **Bottom-aligned controls**: All tabs, toolbar controls, menus, and the omnibox reside within comfortable thumb reach at the bottom of the screen — [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java), [`activity_main.xml`](app/src/main/res/main/layout/activity_main.xml).
- **Tabs management**: Grid/card-based overview supporting horizontal scrolling, quick close, and incognito distinction — [`AdapterTabs.java`](app/src/main/java/de/baumann/browser/feature/browser/AdapterTabs.java), [`BrowserContainer.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserContainer.java), [`dialog_overview.xml`](app/src/main/res/main/layout/dialog_overview.xml).
- **Home view with shortcuts**: Fast-access grid for bookmarked favorites with customizable tiles — [`HomePageView.java`](app/src/main/java/de/baumann/browser/feature/browser/HomePageView.java), [`AdapterHomeShortcuts.java`](app/src/main/java/de/baumann/browser/feature/browser/AdapterHomeShortcuts.java), [`layout_home_page.xml`](app/src/main/res/main/layout/layout_home_page.xml).
- **Search & Omnibox**: Unified search and domain navigation dialog — [`AdapterSearch.java`](app/src/main/java/de/baumann/browser/feature/dialogs/AdapterSearch.java), [`dialog_search.xml`](app/src/main/res/dialogs/layout/dialog_search.xml).
- **Customizable overflow menu**: Fully configurable menu items with drag-and-drop ordering — [`AdapterMenu.java`](app/src/main/java/de/baumann/browser/feature/dialogs/AdapterMenu.java), [`Settings_Menu.java`](app/src/main/java/de/baumann/browser/feature/settings/Settings_Menu.java), [`dialog_menu_overflow.xml`](app/src/main/res/dialogs/layout/dialog_menu_overflow.xml).
- **Background links**: Open links in background tabs without interrupting your reading flow — [`BrowserUnit.java`](app/src/main/java/de/baumann/browser/feature/common/BrowserUnit.java).
- **Session restore**: Automatically restore open tabs upon restarting the browser — [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java).
- **Keep screen on**: Prevent screen timeout during long reading sessions — [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java).

#### Recent UI Refinements (Nov 2026)

A focused pass of Material You polish across all core layouts:

- **Bottom toolbar** (`activity_main.xml`): Progress bar is now full-width with rounded corners and a themed color; `fab_showAppBar` gains proper elevation (4 dp); all toolbar FABs get `pressedTranslationZ` for tactile press feedback; button row is vertically centred.
- **Tab overview** (`dialog_overview.xml`): Header elevation raised to 4 dp for a clearer visual separation from content; bottom dock padding increased for better breathing room; new-tab FAB container moves 4 dp further from the edge.
- **Tab cards** (`item_tab_card.xml`): Card elevation 2 → 3 dp; header height 44 → 48 dp (Material3 minimum touch target); close button 34 → 36 dp for easier one-handed dismissal.
- **Home page** (`layout_home_page.xml`): Top padding reduced (48 → 40 dp); search bar margins tightened (28 → 20/24 dp); search bar gets a subtle 1 dp elevation lift.
- **Fast-toggle dialog** (`dialog_fast_toggle.xml`): Screen-on and background-links checkboxes refactored from bare `CheckBox` elements into proper `RelativeLayout` rows with label `TextView` + end-aligned checkbox, matching the style of all other toggle rows.
- **List items** (`item_list.xml`): Vertical padding 5 → 6 dp; icon card corner radius 4 → 8 dp for a softer, more modern look.
- **Menu items** (`item_menu.xml`): Root width fixed to `match_parent`; icon given an explicit 22 × 22 dp size.
- **Snackbar** (`custom_snackbar.xml`): Hardcoded `#FFFFFF` text colours replaced with `?attr/colorOnInverseSurface` / `?attr/colorOutline` for correct light/dark theme behaviour.

----

### Bookmarks & History

Organize your bookmarks with filters. You can set custom names for each filter. Long press the bookmark icon in the toolbar to get fast access to your favorite bookmarks.

- **Storage & Database**: SQLite record actions for bookmarks and browsing history — [`RecordAction.java`](app/src/main/java/de/baumann/browser/feature/data/RecordAction.java), [`RecordHelper.java`](app/src/main/java/de/baumann/browser/feature/data/RecordHelper.java), [`RecordUnit.java`](app/src/main/java/de/baumann/browser/feature/data/RecordUnit.java), [`Record.java`](app/src/main/java/de/baumann/browser/feature/data/Record.java).
- **Record Adapter**: Visual list rendering with custom tag icons and favicons — [`AdapterRecord.java`](app/src/main/java/de/baumann/browser/feature/data/AdapterRecord.java), [`FaviconHelper.java`](app/src/main/java/de/baumann/browser/feature/data/FaviconHelper.java).
- **Bookmark Filters**: Filter bookmarks by custom category tags and colors — [`Settings_Filter.java`](app/src/main/java/de/baumann/browser/feature/settings/Settings_Filter.java), [`Fragment_settings_Filter.java`](app/src/main/java/de/baumann/browser/feature/settings/Fragment_settings_Filter.java).

----

### Gestures

You can assign nearly twenty different gestures to the toolbar and the toolbar buttons. Each in four directions. You can also trigger events by long pressing the toolbar buttons. So you have up to ten different gestures to control FOSS Browser. Supported is for example: load last website, switch tab, reload, open bookmarks, new incognito tab, ... and many more.

- **Gesture Detection & Dispatching**: Touch listener and gesture router — [`SwipeTouchListener.java`](app/src/main/java/de/baumann/browser/feature/browser/SwipeTouchListener.java), [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java) (`performGesture`).
- **Gesture Configuration**: Settings preference fragment for directional gestures — [`Settings_Gesture.java`](app/src/main/java/de/baumann/browser/feature/settings/Settings_Gesture.java), [`Fragment_settings_Gesture.java`](app/src/main/java/de/baumann/browser/feature/settings/Fragment_settings_Gesture.java).

----

### Backups

Backup all your important data and settings (bookmarks, history, websites-settings and so on) on your SD-card. You can also back up FOSS Browser settings. Backups can even be restored on a fresh install or another device (if you copy the backup files to the new device).

- **Backup Management**: Export and import database records, preferences, and lists — [`BackupUtils.java`](app/src/main/java/de/baumann/browser/feature/settings/BackupUtils.java), [`Settings_Backup.java`](app/src/main/java/de/baumann/browser/feature/settings/Settings_Backup.java).

----

### Pluggable Rendering Engines (Servo & WebView)

FOSS Browser decouples UI controls from the underlying rendering engine using a modular abstraction layer:

- **Engine Abstraction Layer**:
  - [`BrowserEngine.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/BrowserEngine.java): Core engine interface defining lifecycle, navigation, preferences, and view binding.
  - [`BrowserEngineClient.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/BrowserEngineClient.java): Engine-agnostic callback interface for page lifecycle, alerts, downloads, and SSL events.
  - [`UrlProcessor.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/UrlProcessor.java): URL routing, query stripping, and internal scheme resolution.
- **Android System WebView**:
  - [`WebViewEngine.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/WebViewEngine.java): Default, production-tested engine leveraging system WebKit/Blink with legacy feature parity.
- **Servo Web Engine (Experimental)**:
  - [`ServoEngine.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/ServoEngine.java): Independent, embeddable web engine written in Rust, utilizing parallel layout and rendering via OpenGL ES through [`servoview-local/`](servoview-local/README.md).
- **Composition Host**:
  - [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java): Container `FrameLayout` hosting either engine dynamically based on user configuration.
- **User Switching**: Choose between Android System WebView and Servo directly in settings (**Settings > Browser Settings > Web rendering engine**).
- For developer guides, architecture details, and how to activate Servo with the AAR binary, see [AGENT_REFERENCE.md](AGENT_REFERENCE.md) and [servoview-local/README.md](servoview-local/README.md).

----

### Security Architecture & Audit

FOSS Browser incorporates a defense-in-depth security model verified by automated regression tests and security audits:

- **Security Documentation Hub**:
  - [Security Policy & Vulnerability Disclosure](security/SECURITY.md): Reporting process, response SLAs, and supported versions.
  - [Six-Month Security Audit Report (2026)](security/AUDIT_REPORT_2026.md): Full audit findings, upstream Servo assessment, and resolved vulnerabilities.
  - [Architectural Remediation Plan](security/REMEDIATION_PLAN.md): Step-by-step security enhancements and verification status.
  - [Threat Model & Isolation Architecture](security/THREAT_MODEL.md): Trust boundaries, threat actors, and security invariants.
- **Per-Origin Web Permissions**: Explicit interactive confirmation dialogs for Camera, Microphone, and Location access with site memory — [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java).
- **Incognito Multi-Profile Isolation**: AndroidX WebKit Multi-Profile (`WebViewFeature.MULTI_PROFILE`) assigns incognito tabs to a separate, isolated profile with zero disk persistence — [`WebViewEngine.java`](app/src/main/java/de/baumann/browser/feature/browser/engine/WebViewEngine.java).
- **Network Security Configuration**: Cleartext traffic strictly anchored to system CAs and HTTPS enforced for filter list downloads — [`network_security_config.xml`](app/src/main/res/common/xml/network_security_config.xml), [`AdBlock.java`](app/src/main/java/de/baumann/browser/feature/browser/AdBlock.java).
- **Hardened Error Handling & Navigation**: SSL errors default to secure abort; dangerous schemes (`file:`, `content:`, `javascript:`) are filtered from intent launching — [`NinjaWebView.java`](app/src/main/java/de/baumann/browser/feature/browser/NinjaWebView.java).
- **Data Integrity & Injection Defenses**: Parameterized SQL queries in [`RecordAction.java`](app/src/main/java/de/baumann/browser/feature/data/RecordAction.java), Zip Slip traversal defenses in [`BackupUtils.java`](app/src/main/java/de/baumann/browser/feature/settings/BackupUtils.java), and DOM XSS escaping in [`BrowserActivity.java`](app/src/main/java/de/baumann/browser/feature/browser/BrowserActivity.java).
- **Regression Test Suite**: Automated unit tests verifying path traversal prevention, HTTPS enforcement, and scheme isolation — [`SecurityRegressionTest.java`](app/src/test/java/de/baumann/browser/feature/security/SecurityRegressionTest.java).

----

### Project Structure & Key Files

```
app/src/main/java/de/baumann/browser/feature/
├── browser/
│   ├── BrowserActivity.java          # Main browser activity, UI lifecycle, toolbar & dialogs
│   ├── NinjaWebView.java             # Hybrid browser view container (FrameLayout + AlbumController)
│   ├── BrowserController.java        # Interface for activity-level tab & UI interactions
│   ├── BrowserContainer.java         # Static registry managing active tab controllers
│   ├── AlbumController.java          # Tab controller abstraction interface
│   ├── AdapterTabs.java              # RecyclerView adapter for open tab cards
│   ├── HomePageView.java             # Home start page view (normal & incognito modes)
│   ├── AdapterHomeShortcuts.java     # Grid adapter for home view shortcuts
│   ├── AdBlock.java                  # Host-based ad-blocking filter engine
│   ├── BannerBlock.java              # Injected CSS/JS cookie banner blocker
│   ├── List_standard.java            # Domain profile whitelist/blacklist persistence
│   ├── NinjaDownloadListener.java    # Download handling & storage permissions
│   ├── SwipeTouchListener.java       # Directional swipe & touch gesture detection
│   └── engine/
│       ├── BrowserEngine.java        # Core rendering engine interface
│       ├── BrowserEngineClient.java  # Engine callback interface
│       ├── WebViewEngine.java        # System WebView implementation
│       ├── ServoEngine.java          # Rust Servo engine implementation
│       └── UrlProcessor.java         # URL validation, query cleaner & protocol routing
├── common/
│   ├── BrowserUnit.java              # Browser helpers, URL queries & tab opening
│   ├── HelperUnit.java               # UI helpers, dialog builders & system utils
│   ├── NinjaToast.java               # Custom styled toast alerts
│   └── PermissionUtils.java          # Runtime Android permissions manager
├── data/
│   ├── RecordAction.java             # Database operations for bookmarks and history
│   ├── RecordHelper.java             # SQLiteOpenHelper for records database
│   ├── RecordUnit.java               # Database table schemas and constants
│   ├── Record.java                   # Data model for bookmark and history entries
│   ├── AdapterRecord.java            # List adapter for bookmarks & history
│   └── FaviconHelper.java            # Favicon retrieval, caching & bitmap binding
├── dialogs/
│   ├── AdapterMenu.java              # Adapter for custom overflow menu grid
│   ├── AdapterSearch.java            # Adapter for search suggestions & history
│   ├── CustomRedirectsDialog.java    # Dialog fragment for URL redirect management
│   └── MenuItem.java                 # Data model for menu items
└── settings/
    ├── Settings_Activity.java        # Preference activity container
    ├── Fragment_settings.java        # Root settings fragment
    ├── Settings_Menu.java            # Menu item customization & reordering
    ├── Settings_Profile.java         # Domain & standard privacy profile editor
    ├── Settings_ProfileList.java     # Configured domain profiles list
    ├── Settings_Gesture.java         # Toolbar & nav button gesture assignment
    ├── Settings_Filter.java          # Bookmark filter categories editor
    ├── Settings_Backup.java          # Backup & restore preferences
    ├── Settings_Clear.java           # Cache, history & cookie wipe options
    ├── BackupUtils.java              # SD-card backup exporter/importer
    ├── DeleteUtils.java              # Automated database and cache cleaner
    ├── CustomSearchesHelper.java     # Custom search engines persistence
    └── CustomRedirectsHelper.java    # Custom URL redirects persistence

security/
├── SECURITY.md                       # Security policy, supported versions & disclosure guidelines
├── AUDIT_REPORT_2026.md              # Six-month security audit findings, CVEs & resolved issues
├── REMEDIATION_PLAN.md               # Architecture remediation plan and verification status
└── THREAT_MODEL.md                   # Threat actors, trust boundaries & isolation guarantees

app/src/test/java/de/baumann/browser/feature/security/
└── SecurityRegressionTest.java       # Unit tests for Zip Slip, HTTPS checks & scheme blocking
```

----

### Building from Source

Prerequisites: Android SDK (API 29–37), JDK 17.

```bash
# Compile and check Java code
./gradlew compileDebugJavaWithJavac

# Build Debug APK (output in app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

----

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="50"/>](https://f-droid.org/packages/de.baumann.browser/)
[<img src="https://www.paypalobjects.com/de_DE/DE/i/btn/btn_donateCC_LG.gif" alt="Donate" height="50"/>](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NP6TGYDYP9SHY)