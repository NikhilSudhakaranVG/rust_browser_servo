<p align="center">
<img src="fastlane/metadata/android/en-US/images/featuresGraphic.png" alt="FOSS Browser" width="600"/>
</p>

__[README](README.md)__ | __[WIKI](wiki/)__ | __[FAQs](FAQs.md)__ | __[Changelog](CHANGELOG.md)__ | __[Privacy](PRIVACY.md)__ | __[License](LICENSE.md)__ | __[Code of conduct](CODE_OF_CONDUCT.md)__

----

FOSS Browser is a privacy-focused, open-source Android web browser optimized for one-handed use. It features a modern, pluggable engine architecture supporting both the standard Android System WebView and experimental integration with the next-generation, Rust-powered [Servo browser engine](https://servo.org/).

----

### Privacy

FOSS Browser uses profiles to protect your privacy. For each domain or standard profile, you can enable or disable: AdBlock, JavaScript, cookies, fingerprint protection, and much more. Saved domain settings always overwrite the standard profile, allowing trusted domains (e.g. `example.com` ⇒ custom settings) to open with specific permissions while remaining protected on unknown sites.

_More privacy features:_

- **Built-in AdBlock**: Automatically updated filter list blocking unwanted network requests (AdBlock hosts from [Steven Black](https://github.com/StevenBlack/hosts)).
- **Third-party cookies**: Block third-party trackers with independent cookie policy control.
- **Cookie banner blocker**: Automatically block intrusive consent banners.
- **Fingerprint protection**: Defend against browser and hardware fingerprinting (including WebRTC IP leaks).
- **Auto-wipe on exit**: Automatically clear cache, cookies, and history upon closing the app.
- **Android Autofill**: Enable or disable system autofill integration.
- **Custom search engines**: Choose between privacy-respecting search providers or configure custom search URLs.
- **Zero data collection**: FOSS Browser does not collect or transmit any telemetry ([Privacy policy](PRIVACY.md)).

----
### UI & Handling

FOSS Browser is built with Material You design libraries. Following system day/night mode and a wallpaper-based dynamic theme are just two features of this interface. The UI is purposefully optimized for one-handed use:

- **Bottom-aligned controls**: All tabs, toolbar controls, menus, and the omnibox reside within comfortable thumb reach at the bottom of the screen.
- **Background links**: Open links in background tabs without interrupting your reading flow.
- **Session restore**: Automatically restore open tabs upon restarting the browser.
- **Keep screen on**: Prevent screen timeout during long reading sessions.

----
### Bookmarks filter

Organize your bookmarks with filters. You can set custom names for each filter. Long press the bookmark icon in the toolbar to get fast access to your favorite bookmarks.

----
### Gestures

You can assign nearly twenty different gestures to the toolbar and the toolbar buttons. Each in four directions. You can also trigger events by long pressing the toolbar buttons. So you have up to ten different gestures to control FOSS Browser. Supported is for example: load last website, switch tab, reload, open bookmarks, ... and many more.

----
### Backups

Backup all your important data and settings (bookmarks, history, websites-settings and so on) on your SD-card. You can also back up FOSS Browser settings. Backups can even be restored on a fresh install or another device (if you copy the backup files to the new device).

----
### Pluggable Rendering Engines (Servo & WebView)

FOSS Browser decouples UI controls from the underlying rendering engine using a modular abstraction layer:

- **Android System WebView**: Default, production-tested engine leveraging the system WebKit/Blink implementation.
- **Servo Web Engine (Experimental)**: An independent, embeddable web engine written in Rust, utilizing parallel layout and rendering via OpenGL ES.
- **Runtime Switching**: Switch engines on demand via Developer Options (`pref_use_servo`).
- For developer guides, architecture details, and how to activate Servo with the AAR binary, see [AGENT_REFERENCE.md](AGENT_REFERENCE.md) and [servoview-local/README.md](servoview-local/README.md).

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