# Android WebView Test App

A small Android WebView test harness with:

- Editable URL field
- Back, forward, and reload controls
- JavaScript and DOM storage enabled
- Cookie and third-party cookie support
- Page progress and status display
- Console messages written to Logcat under `WebViewConsole`
- HTTP and WebView errors written to Logcat under `WebViewTest`
- Chrome remote debugging enabled in debug builds
- HTTP enabled for local development testing
- SSL certificate errors rejected
- Non-HTTP(S) links delegated to Android

## Run

1. Open the project in a current Android Studio release.
2. Allow Gradle sync to install Android SDK 37 if necessary.
3. Run the `app` configuration on an emulator or device.

## Local development URLs

- Android emulator to host machine: `http://10.0.2.2:PORT`
- Physical device: use your computer's LAN IP and bind the server to `0.0.0.0`

## Inspect the WebView

With a debug build running and USB debugging enabled, open `chrome://inspect/#devices` in desktop Chrome.

## Security note

This is a development test harness. Cleartext HTTP and third-party cookies are deliberately enabled. Do not reuse those settings unchanged in a production app.
