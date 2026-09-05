# Play Console publishing checklist

The repository now prepares the technical and creative upload files. The items below require the Play Console account owner or a policy decision before submission.

## Release artifact

- [x] Package name: `com.chrisawad.glowwords`
- [x] Target SDK: 36
- [x] Stable signing configuration exists in GitHub Actions
- [x] Release workflow builds a signed Android App Bundle (`app-release.aab`)
- [ ] Run the release workflow and download the signed `.aab`
- [ ] Opt in to Play App Signing when creating the first release
- [ ] Upload to an internal test track and complete a smoke test before production
- [ ] If this is a personal developer account created after November 13, 2023, complete Google's required closed-testing period before production access

## Store listing

- [x] App name, short description, and full description drafted
- [x] 512 × 512 app icon
- [x] 1024 × 500 feature graphic
- [x] Six final 1080 × 1920 phone screenshots with matching alt text
- [ ] Add tablet screenshots only if tablets are included in the supported-device plan
- [ ] Supply support email (required)
- [ ] Supply support website (recommended)
- [ ] Publish the privacy policy at a stable public URL and enter that URL in Play Console

## App content declarations

- [ ] Ads: declare **No** (the inspected app contains no ad SDK or ad UI)
- [ ] App access: declare that all functionality is available without login
- [ ] Complete the IARC content-rating questionnaire
- [ ] Choose the target age groups deliberately. The UI is designed for ages 5–6, 7–8, 9–10, 11–12, and 13+, so any selection including children triggers the Families Policy requirements
- [ ] Complete the Families Policy review, including child-appropriate content and SDK checks
- [ ] Complete the Data safety form after confirming the deployed server and speech-recognition data flows
- [ ] Complete the Data deletion declaration consistently with the final Data safety answers
- [ ] Complete the Government apps, Financial features, Health apps, and News declarations as **No**, if Play Console presents them and the app remains unchanged
- [ ] Confirm whether the game should be included in Google Play Games on PC; free games may be included by default

## Data-safety facts to verify

These are implementation observations, not final legal declarations:

- The Android shell requests `INTERNET`, `MODIFY_AUDIO_SETTINGS`, and optional `RECORD_AUDIO` permissions.
- The app uses HTTPS and rejects SSL certificate errors.
- No account, advertising SDK, analytics SDK, location access, contacts access, or payment code was found.
- The WebView enables DOM storage and first-party cookies for the Glow Words origin.
- The game requests `/api/words` and falls back to bundled word lists if that request fails.
- The optional pronunciation activity uses browser/Android speech recognition. Confirm whether the selected speech service sends voice audio off-device and reflect that accurately in both Data safety and the privacy policy.
- Confirm what request logs, IP addresses, or other telemetry are retained by the production host at `glow.chrisawad.com`.

Google states that the developer is responsible for complete and accurate Data safety declarations, including third-party services. Do not submit the draft answers until the hosting and speech-processing behavior are confirmed.
