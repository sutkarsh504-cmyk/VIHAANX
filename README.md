# Integrity Emergency Care — Android

An Android WebView app that displays https://integrity-emergency-care-hazel.vercel.app/ with its existing design. Website UI changes appear in the app automatically. The website is loaded live; internet is required. Android 8.0 (API 26) or newer is supported. Package ID: `care.integrity.emergency`.

## Install the test app

Transfer `Integrity-Emergency-Care-debug.apk` to your Android phone, open it, and allow installation from the app you used to open the APK if Android prompts. Launch **Integrity Emergency Care**. Allow location when needed to exercise the location flow. The APK is debug-signed for testing, not a Play Store release.

## Included Android behavior

- Existing patient and hospital UI, fonts, imagery, and navigation from the live site.
- Android back button follows the site's visible back control, then browser history, then exits.
- Location permission requested only when the website requests location; approximate location is accepted.
- System file picker for certificate and profile-image uploads. Selecting a file does not establish server-side storage; the website determines what happens next.
- Phone links open the dialer for the user to place a call. No direct-call permission.
- External HTTPS, email and map links open their corresponding apps.
- Page-loading indicator and a retry screen for main-page network/HTTP failures.
- Website local storage persists across launches. Android app backup is disabled.
- HTTPS only, mixed content blocked, no privileged JavaScript bridge, no TLS bypass.
- System bar and keyboard insets keep content clear of Android controls.

The launcher uses a provisional medical-cross icon. Replace it with an approved square brand icon before publishing.

## Existing website limitations

Inspection of the publicly served `app.js` and `patient-hospitals.js` found:

- OTPs are generated in JavaScript and displayed in an alert, not sent by an authentication service.
- Hospital profile, registration status, admin code and ICU edits use device-local browser storage, not shared server records.
- The patient hospital list is a hardcoded demonstration list. Distances and ICU bed counts are randomly generated. They are **not verified availability or reliable nearby-hospital distances**.

These behaviors are inherited from the live website. This Android build does not create a backend or turn demonstration data into real medical availability. Use it as a UI/testing build. Real hospital authentication, verified hospital records, server-side file storage and synchronized bed availability must be implemented before operational use.

## Open and build

1. Unzip the project and open its root folder in Android Studio.
2. Install Android SDK Platform 35 and Build Tools 35.0.0. Use JDK 17 or 21.
3. Let Android Studio configure its SDK path and synchronize Gradle.
4. Run `./gradlew assembleDebug lint` (Windows: `gradlew.bat assembleDebug lint`).

The project uses Gradle 8.9 and Android Gradle Plugin 8.7.3, with no third-party application libraries. First build needs internet to download Gradle and build dependencies. `app/build/outputs/apk/debug/app-debug.apk` is the resulting APK.

## Release

Choose your final package ID before publishing. In Android Studio use **Build → Generate Signed App Bundle or APK**, create or select your organization's signing key, and generate a release Android App Bundle. Keep the signing key secure. Add your privacy policy, location/data declarations, approved branding and Play Store listing. Check current Google Play target-SDK and functionality requirements at submission time. This project has not been submitted to Google Play.

## Validation

See `VALIDATION.md` for checks performed and remaining device tests.
