# Calorie ADN

A native Android food diary and meal planner, redesigned in version 1.1 with a warm cream and green interface.

- **Today:** calorie balance, macro totals, water log and today's planned meals.
- **Food diary:** dated history, breakfast/lunch/dinner/snack groups, portions, editing and deletion.
- **Meal plan:** seven editable days, automatic local saving and optional Kerala meal ideas.
- **Settings:** a user-selected calorie goal and encrypted Gemini API-key setup.
- **Scan food:** photo preview, optional portion/ingredient details, explicit estimation and an editable review screen.

Inner screens have a toolbar Back action and support Android's system Back button. Back returns to the parent screen; the app exits only from Today. Existing diary, goal and meal-plan data are retained.

## Run and build

Open this folder in Android Studio. Use the bundled JDK, install SDK Platform 35, and select an Android device or emulator (Android 8.0/API 26 or newer). Gradle 8.9 is used by the included GitHub Actions build. Java and Kotlin compile to Java 17.

Run `gradle assembleDebug` with Gradle installed and the SDK location configured through `ANDROID_HOME` or a local `local.properties` file. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Checks: `gradle lintDebug connectedDebugAndroidTest` with an emulator or phone connected. Instrumented tests cover nested Back navigation, invalid food inputs, food save/edit persistence and malformed/non-food AI responses. Test entries are removed by restoring the original diary after each test.

The included GitHub Actions workflow also builds and uploads a debug APK. A Play Store release requires a separate release-signing setup.

## Photo AI

1. In **Settings → Photo AI**, save your Gemini API key and an image-capable model available to your Google project.
2. Tap **Test saved connection**. This sends a small text request to Google and may count toward quota/billing.
3. Open **Scan food**, select a meal photo and optionally describe known portions or ingredients.
4. Tap **Estimate nutrition**. The selected photo and description are sent to Google.
5. Review and correct food, portion, calories, macros and meal category before saving to the diary.

The default model is `gemini-3.8-flash`, verified against Google's model documentation on 19 October 2026. The model name is configurable because model availability and project access can change. Requests use structured JSON output, validate nutrition values, reject non-food results and handle key, quota, model and connectivity errors. Cancelling or leaving the scanner disconnects the active request and ignores stale responses.

Gemini identifies likely visible foods and **estimates** their nutrition. It cannot measure exact weight, hidden oils or recipe ingredients from a photo. Changing a portion in the review screen requires adjusting nutrition too. No food is saved automatically. Live API success requires the user's key, model access, internet and available quota; it is not guaranteed by saving a key.

The key is encrypted using Android Keystore, and a legacy plain-text key is migrated when possible. App backup is disabled. Diary and plan data are stored locally; there is no cloud sync. Never put a shared secret key into source control or a publicly distributed APK. Users supply their own keys in this app.

This is a tracking and planning app. It does not prescribe a personalized diet; the initial calorie target and sample meals are placeholders/ideas.

Sources: [Gemini model](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash), [image understanding](https://ai.google.dev/gemini-api/docs/image-understanding), [structured output](https://ai.google.dev/gemini-api/docs/structured-output).
