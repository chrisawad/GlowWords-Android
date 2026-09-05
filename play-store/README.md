# Glow Words Play Store package

This directory contains the upload-ready visual assets and English (US) listing copy for Glow Words.

## Upload-ready graphics

| Asset | File | Play specification |
| --- | --- | --- |
| App icon | `graphics/icon-512.png` | 512 × 512, 32-bit PNG, under 1 MB |
| Feature graphic | `graphics/feature-graphic-1024x500.png` | 1024 × 500, opaque 24-bit PNG |
| Phone screenshots | `screenshots/phone/*.png` | 1080 × 1920, opaque PNG, actual game UI |

The first three phone screenshots should show actual gameplay so the game remains eligible for screenshot-led recommendation surfaces. Google Play accepts up to eight phone screenshots.

The generated launcher artwork is also installed under `app/src/main/res` and referenced by `AndroidManifest.xml`.

## Listing materials

- `listing/en-US.md`: title, descriptions, category, and asset alt text.
- `listing/publishing-checklist.md`: remaining Play Console declarations and owner-supplied values.
- `listing/privacy-policy-draft.md`: a review-ready draft that still needs a contact address, publication URL, and legal/privacy review.

## Rebuild the exported artwork

Run this from the Android project root with Java 17 or later:

```bash
java play-store/tools/PrepareStoreArt.java .
java play-store/tools/VerifyStoreAssets.java .
```

The exporter reads the checked-in PNG masters from `source/`, writes the exact Play icon and feature graphic dimensions, and regenerates all legacy launcher densities. The Android 8+ adaptive launcher icon uses the same safe-area-aware master. The verifier checks every upload graphic's dimensions, alpha mode, count, and applicable file-size limit.

## Current Google references

- [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
- [Google Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications?hl=en)
- [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)
- [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en-EN)
- [Provide information for the Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
