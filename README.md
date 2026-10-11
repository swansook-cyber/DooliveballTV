# Dooliveball TV (Android)

Two WebView tabs:
- ดูบอลสด — https://dooliveball.com/
- ทีวีออนไลน์ — https://dooliveball.com/ดูทีวีออนไลน์

Java, Android API 23+; target API 35; AGP 8.7.3 / Gradle 8.9 / JDK 17.

## Build APK with GitHub Actions
Open **Actions → Build Android APK → Run workflow**. When green, download the `DooliveballTV-debug-apk` artifact and extract `app-debug.apk`.

## Local build
Install Android SDK (platform 35, build tools 35), JDK 17, Gradle 8.9 then run `gradle :app:assembleDebug`.

## Limitations
An APK has not been compiled or physically tested yet. Streaming may be blocked by the website or player restrictions.
