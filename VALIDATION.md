# Validation

## Browser checks

The live website's role, patient emergency-selection and hospital-registration screens were exercised with Chromium at 360×800, 390×844 and 412×915 mobile viewports. All three passed. Nine emergency types were present. The exact back-handler JavaScript extracted from MainActivity was injected into the site and verified to return from both patient and hospital screens to role selection, and to leave the role screen unhandled for native exit.

The included screenshots show the live website at 390×844. They are browser captures, not Android emulator captures.

The cloud browser required an environment-specific certificate accommodation because its traffic passes through an inspection proxy. The shipped app retains normal Android TLS checks and does not bypass certificate errors.

## Android checks

`assembleDebug lint` completed successfully. Android lint reported zero errors and zero warnings. APK signature verification passed (APK Signature Scheme v2). Manifest inspection confirmed package `care.integrity.emergency`, minimum API 26, target API 35, and only internet/coarse location/fine location permissions. The APK is debug-signed. Reports are included in `verification/`.

## Device testing remaining

No Android emulator or physical phone was available for runtime testing. Verify on real Android devices before release:

1. Install, cold launch, rotate, background/resume, and keyboard layout.
2. Patient and hospital navigation using Android back, including overlays and maps.
3. Grant approximate/precise location, deny it, revoke it in settings, and retry.
4. Choose and cancel certificate/photo uploads; check the site's handling of each.
5. Open a phone link and confirm the dialer opens without placing a call automatically.
6. Disable network, relaunch, restore network and use Try again.
7. Confirm local-data behavior across app updates; uninstall removes local records.
8. Test the website's authentication and hospital synchronization again after replacing its demo backend behavior.
