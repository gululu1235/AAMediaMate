# Repository Guidelines

## Project Structure & Module Organization
- Single Android app module in `app/`.
- Kotlin sources: `app/src/main/java/com/gululu/aamediamate`.
- Resources: `app/src/main/res`; manifest: `app/src/main/AndroidManifest.xml`.
- Tests: unit in `app/src/test`; instrumentation in `app/src/androidTest`.
- Gradle config: root `build.gradle.kts`, `settings.gradle.kts`, module `app/build.gradle.kts`.

## Build, Test, and Development Commands
- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
- Install on device/emulator: `./gradlew :app:installDebug`.
- Unit tests (Robolectric/JUnit): `./gradlew testDebugUnitTest`.
- Instrumentation tests (requires device): `./gradlew connectedAndroidTest`.
- Lint and checks: `./gradlew lint` and `./gradlew check`.

## Coding Style & Naming Conventions
- Kotlin “official” style (`gradle.properties`: `kotlin.code.style=official`), 4‑space indent.
- Package remains `com.gululu.aamediamate`. File names match top‑level class (e.g., `MediaBridgeService.kt`).
- Naming: Classes `PascalCase`, methods/properties `lowerCamelCase`, constants `UPPER_SNAKE_CASE`.
- Prefer coroutines over blocking I/O; keep work off main thread.
- Public APIs: add concise KDoc. Keep visibility as small as possible.

## Testing Guidelines
- Frameworks: JUnit 4, MockK, Robolectric (see `app/build.gradle.kts`).
- Place unit tests under `app/src/test/...` named `FooTest.kt`; instrumentation under `app/src/androidTest/...` named `FooInstrumentedTest.kt`.
- Aim to cover new business logic and edge cases; mock Android APIs where helpful.
- Run locally with `testDebugUnitTest`; verify on device with `connectedAndroidTest`.

## Commit & Pull Request Guidelines
- Commits: imperative, concise subject (≤50 chars), explain “what/why”. Example: `Fix media session package name`.
- Reference issues in body (e.g., `Fixes #123`).
- PRs: clear description, testing steps, screenshots for UI changes, and notes on config/signing changes.
- CI hygiene: ensure `assembleDebug`, `lint`, and tests pass before requesting review.

## Security & Configuration Tips
- Do not commit secrets or keystores. Release signing uses `local.properties` keys: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- Keep `applicationId` and min/target SDKs aligned with `app/build.gradle.kts`. JVM target is 11.

## Quick Orientation
- Key classes: `MediaBridgeService`, `MediaControllerManager`, `MediaNotificationListener`, `LyricDisplayManager`, `MainActivity` in `app/src/main/java/com/gululu/aamediamate`.
