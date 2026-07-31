# GlowWords for Android

A focused Android shell for [GlowWords](https://glow.chrisawad.com). The app
opens the game directly in a full-window WebView with no browser or test
controls.

- Application ID: `com.chrisawad.glowwords`
- JavaScript, DOM storage, sound, and microphone-backed word practice enabled
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
