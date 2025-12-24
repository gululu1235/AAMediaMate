# AAMediaMate (Android Auto Media Mate) Project Context

## Project Overview
**AAMediaMate** is an Android application designed to bridge media applications that do not natively support Android Auto (e.g., QQ Music, NetEase Cloud Music, YouTube) to the Android Auto interface. It functions by listening to media notifications on the phone and exposing that metadata and control to Android Auto via a MediaBrowserService. It also features a lyrics display system that overlays lyrics as the song title.

*   **Package Name:** `com.gululu.aamediamate`
*   **Min SDK:** 29 (Android 10)
*   **Target SDK:** 35 (Android 15)
*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose

## Architecture & Key Components

The application is built around a service-based architecture to ensure it runs in the background and interacts properly with the Android system and Android Auto.

### Core Services
*   **`MediaNotificationListener`** (`.MediaNotificationListener`): Extends `NotificationListenerService`. It listens for notifications from active media players on the device to extract metadata (title, artist, album, art) and playback state.
*   **`MediaBridgeService`** (`.MediaBridgeService`): Extends `MediaBrowserServiceCompat`. This is the service that Android Auto connects to. It "bridges" the information captured by the listener to the car's display and forwards transport controls (Play, Pause, Next, Prev) back to the original media app.

### Lyrics System
*   **`LyricsManager`** (`.lyrics.LyricsManager`): Manages the retrieval and synchronization of lyrics.
*   **Providers:** Supports multiple lyrics providers including Musixmatch (via RapidAPI) and LrcApi (Open Source).
*   **`LyricDisplayManager`**: Handles the formatting and updating of the media metadata to display lyrics (e.g., scrolling text in the title field).

### UI (Phone Side)
*   **Toolkit:** Jetpack Compose (Material3).
*   **`MainActivity`**: The entry point for the phone UI.
*   **Screens:**
    *   `BridgedAppsScreen`: Manage which apps are bridged.
    *   `LyricsManagerScreen`: Manage cached lyrics.
    *   `SettingsScreen`: General configuration.
    *   `LyricsEditorScreen`: Manual editing of lyrics.

## Key Files and Directories

*   `app/src/main/java/com/gululu/aamediamate/`: Root source directory.
    *   `Global.kt`: Application-wide constants and state.
    *   `MediaBridgeService.kt`: Core Android Auto integration logic.
    *   `MediaNotificationListener.kt`: Notification interception logic.
    *   `lyrics/`: Lyrics fetching, parsing, and management.
    *   `ui/`: Jetpack Compose UI screens and themes.
*   `app/src/main/res/`: Android resources (layouts, drawables, values).
*   `app/build.gradle.kts`: App module build configuration, dependencies.
*   `AGENTS.md`: Specific context and guidelines for AI agents working on this repo.

## Development & Usage

### Build Commands
*   **Build Debug APK:** `./gradlew assembleDebug`
*   **Install Debug APK:** `./gradlew installDebug`
*   **Run Unit Tests:** `./gradlew testDebugUnitTest`
*   **Run Instrumentation Tests:** `./gradlew connectedAndroidTest`
*   **Lint Checks:** `./gradlew lint`

### Coding Conventions
*   **Style:** Kotlin "official" code style (4-space indent).
*   **Concurrency:** Coroutines are preferred over blocking I/O.
*   **Testing:**
    *   Unit tests in `app/src/test` using JUnit 4, MockK, and Robolectric.
    *   Instrumentation tests in `app/src/androidTest`.
*   **Logging:** Use standard Android `Log` class.

### Android Auto Testing
To test the "Bridge" functionality without a car:
1.  Enable "Unknown Sources" in Android Auto settings (requires Developer Mode in AA app).
2.  Use the "Desktop Head Unit (DHU)" emulator provided by Android Studio/SDK to simulate a car connection.
