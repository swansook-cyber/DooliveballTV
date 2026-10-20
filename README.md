# Vela Sport (Android)

The Android app keeps the original Dooliveball website in a WebView. Its ดูบอลสด and ทีวีออนไลน์ navigation stays on the website. The application ID remains `net.velalab.dooliveballtv` for update compatibility.

Java, Android API 23+; target API 35; AGP 8.7.3 / Gradle 8.9 / JDK 17. The launcher icon and TV banner are generated from the original artwork in `assets/` and checked into `app/src/main/res/drawable/` so local builds use the same images as CI.

## Android TV remote

- D-pad moves a visible focus outline among web controls in the direction pressed.
- OK sends a real touch to the focused control; Back closes full-screen video or returns to the previous web page.
- When a video iframe is selected, pointer mode turns on automatically. D-pad moves the pointer and OK taps it. Hold OK, or press Menu/Info, to toggle pointer mode at any time.
- Channel/Page Up and Down scroll the page. Touch interaction on phones remains native WebView behavior.

## Build APK with GitHub Actions
Push a branch or open **Actions → Build Android APK → Run workflow**. The workflow builds, runs Android lint, checks package/version/label/signature, and uploads the `VelaSport-v1.3.0-debug-apk` artifact.

## Local build
Install Android SDK (platform 35, build tools 35), JDK 17, Gradle 8.9 then run `gradle :app:assembleDebug :app:lint`.

## Signing and store upload

The debug APK from GitHub Actions is for testing only. The previous v1.2.0 Actions APK uses an Android Debug certificate. A new Actions runner normally creates a different debug key, so a new debug APK cannot be assumed to update an installed copy. To upload an APK that updates existing installations, sign the release APK with the exact private key used for the APK distributed by the store, then verify the signing certificate SHA-256 fingerprint with `apksigner verify --print-certs`. The store-distributed APK or its fingerprint and the corresponding private keystore are required before claiming update compatibility.

Streaming availability still depends on the website and third-party player.
