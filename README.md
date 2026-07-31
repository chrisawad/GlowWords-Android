# GlowWords for Android

A focused Android shell for [GlowWords](https://glow.chrisawad.com). The app
opens the game directly in a full-window WebView with no browser or test
controls.

- Application ID: `com.chrisawad.glowwords`
- JavaScript and DOM storage enabled
- Android text-to-speech fallback for word and letter playback in WebView
- Microphone-backed word practice enabled for the deployed GitHub Pages origin
- Android back gesture/button navigates WebView history
- Non-HTTP(S) links are delegated to the appropriate Android app
- HTTPS-only networking with SSL certificate errors rejected

## Run

1. Open the project in a current Android Studio release.
2. Allow Gradle sync to install Android SDK 36 if necessary.
3. Run the `app` configuration on an emulator or device.

## Build

```bash
gradle --no-daemon assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Stable release signing

Pull requests build disposable debug APKs for validation. Pushes to `main` and
manual workflow runs build `app-release.apk` with a long-lived signing key, so
one stable release can update another without uninstalling the app. The
workflow also assigns an increasing CI version code and verifies the APK
certificate before uploading the `GlowWords-Android-release` artifact.

The expected release certificate SHA-256 fingerprint is:

```text
CF:A1:49:CB:D3:B2:60:95:1C:1E:A8:55:28:A2:A0:04:
AE:2E:CC:48:79:55:00:41:F2:F2:A7:79:F8:17:A3:DC
```

The signing material is supplied only through these encrypted GitHub Actions
repository secrets:

- `GLOWWORDS_KEYSTORE_BASE64`
- `GLOWWORDS_KEYSTORE_PASSWORD`
- `GLOWWORDS_KEY_ALIAS`
- `GLOWWORDS_KEY_PASSWORD`

Never commit the keystore or its passwords. Keep a secure backup outside the
repository; losing the private key prevents compatible updates. APKs installed
from the older per-run debug keys must be uninstalled once before installing
the first stable release.
