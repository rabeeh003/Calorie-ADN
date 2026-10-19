# Verification — Calorie ADN 1.1

19 October 2026 (Asia/Dubai)

- `assembleDebug assembleDebugAndroidTest lintDebug`: passed. Lint reported 0 errors and 10 warnings.
- Direct device instrumentation on the API 35 emulator: **OK (3 tests)**, 13.081 seconds.
  - Nested Back navigation returns through parent pages without finishing the activity.
  - Food input validation, saving, editing and persisted values.
  - Rejection of malformed, incomplete, non-food and invalid-nutrition AI results.
- Actual Android Back key from Food diary returned to Today, verified through the UI hierarchy.
- Today, Food diary and Scan food were visually reviewed at a 360 dp phone width. Screenshots are in `screenshots/`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `4347390791F772118F6E0EA808BA81CE6C91F977BCE3508615EA39B7599839E6`

The first Gradle-managed device run failed to attach to the emulator and ran zero tests. After correcting the test runner manifest and separating builds from emulator execution to reduce memory pressure, the direct instrumentation run above passed all three tests.

Live Gemini requests were not tested because no user API key was supplied. The connection test, model selection and error handling are implemented; actual model access, quota and recognition quality must be checked with the user's key and meal photos.
