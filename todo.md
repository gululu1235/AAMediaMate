# AAMediaMate Development TODO (Refactoring & Enhancement)

This file tracks the development tasks based on the project specification and new requirements focusing on code quality, testing, and UI improvements.

## Completed Tasks ✅

- [x] **Single Responsibility Principle (SRP) for `MediaBridgeSessionManager`:**
    - [x] Decomposed the class by creating `MediaStateUpdater` and `LyricDisplayManager`.
- [x] **Unit Testing for Core Algorithms:**
    - [x] Created unit tests for `LyricSyncEngine`.
    - [x] Created unit tests for `LyricCache` and `LyricsRepository`.
    - [x] Configured the test environment with MockK and Robolectric.
- [x] **Handle Missing Metadata Gracefully:**
    - [x] Modified `MediaStateUpdater` to prevent displaying "<unknown>" for artist or album.

## Future Architectural Improvements (Backlog)

**Goal:** Continue improving the codebase for better maintainability and scalability.

- [ ] **SRP - Further Decomposition:**
    - [ ] **`MediaInformationRetriever`:** Isolate data fetching. Move the album art composition logic (drawing the app icon) into a separate `AlbumArtCompositor` class.
    - [ ] **`LyricsManager`:** Clearly separate concerns. The manager should coordinate, while dedicated classes handle specific tasks:
        - `LyricsFetcher`: Manages fetching lyrics from various `LyricsProvider` instances.
- [ ] **Dependency Inversion Principle (DIP):**
    - [ ] **Dependency Injection:** Integrate a dependency injection framework (like Hilt or Koin) or use manual injection to provide dependencies.
    - [ ] **Use Interfaces:** Ensure all major components (`LyricsRepository`, `SettingsManager`, etc.) are accessed through interfaces.

## Future Testing (Backlog)

- [ ] **Expand `LyricSyncEngine` Tests:**
    - [ ] Test correct behavior when the user seeks forward and backward in the track.
    - [ ] Test graceful handling of out-of-order or malformed timestamps in LRC data.
    - [ ] Test edge cases, such as empty or single-line lyric files.
- [ ] **Refactoring-Related Tests:**
    - [ ] Write targeted unit tests for other new classes created during refactoring.