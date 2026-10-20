# Vela Sport (Android)

The Android app keeps the original Dooliveball website in a WebView. Its ดูบอลสด and ทีวีออนไลน์ navigation stays on the website. The application ID remains `net.velalab.dooliveballtv` for update compatibility.

Java, Android API 23+; target API 35; AGP 8.7.3 / Gradle 8.9 / JDK 17. The launcher icon is byte-for-byte identical to `assets/vela-sport-original.png`. The 16:9 TV banner contains the same original pixels centered on a matching background, with no stretch or crop. Both PNG drawables are checked into `app/src/main/res/drawable/`.

## Android TV remote

- D-pad moves a visible focus outline among web controls in the direction pressed.
- OK sends a real touch to the focused control; Back closes full-screen video or returns to the previous web page.
- When a video iframe is selected, pointer mode turns on automatically. D-pad moves the pointer and OK taps it. Hold OK, or press Menu/Info, to toggle pointer mode at any time.
- Channel/Page Up and Down scroll the page. Touch interaction on phones remains native WebView behavior.

## Build APK with GitHub Actions
Push a branch or open **Actions → Build Android APK → Run workflow**. The workflow checks the original PNG, runs the TV navigation tests, builds, runs Android lint, checks package/version/label/signature, and uploads the `VelaSport-v1.3.1-debug-apk` artifact.

## Local build
Install Android SDK (platform 35, build tools 35), JDK 17, Gradle 8.9 then run `gradle :app:assembleDebug :app:lint`.

## Signing and store upload

The debug APK from GitHub Actions is for fresh-install testing. The store-distributed v1.3.0 (4) APK has signer certificate SHA-256 `92c8f63a358aa0677c16e2dd775943ba25aad4f4693d8a01ca2627851b2d0e57`. To update it in place, sign v1.3.1 with the matching private key, then verify the certificate with `apksigner verify --print-certs`. A newly generated debug key will not update the store-distributed APK.

Streaming availability still depends on the website and third-party player.
