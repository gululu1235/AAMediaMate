# AAMediaMate 功能规格说明 (Functional Specification)

## 1. 概述

AAMediaMate 是一个专为 Android Auto 设计的媒体增强工具。其核心功能是作为一个“桥梁”，捕获并同步来自第三方音乐应用的媒体播放信息（如歌曲名称、艺术家、专辑封面、播放进度等）到车载娱乐系统。

与标准的媒体集成不同，本应用通过创新的方式增强了用户体验，特别是加入了动态歌词显示功能，允许用户在 Android Auto 界面上看到与当前播放歌曲同步的歌词。

本文档旨在详细描述 AAMediaMate 的各项功能、技术架构和核心组件，为开发者提供一份清晰的蓝图，以便能够独立实现一个功能类似的应用。

## 2. 核心功能

- **媒体会话桥接**：自动检测并捕获来自其他应用的媒体播放会话（如 Spotify, YouTube Music, Apple Music 等）。
- **信息同步至 Android Auto**：将当前播放的媒体信息（歌曲、艺术家、专辑、封面、进度）格式化并显示在 Android Auto 的标准媒体播放界面上。
- **媒体控制转发**：接收来自 Android Auto 的媒体控制指令（播放、暂停、上一首、下一首、快进/快退、拖动进度条），并将其准确地转发给原始的音乐应用。
- **动态歌词系统**：
    - **歌词获取**：支持从多种在线歌词源（如 Musixmatch API、自定义 LRC API）自动获取当前歌曲的歌词。
    - **歌词缓存**：将获取到的歌词在本地进行缓存，减少网络请求，并允许离线使用。
    - **歌词同步**：内置歌词同步引擎，能够根据当前歌曲的播放进度，逐行高亮显示歌词。
    - **歌词管理**：提供歌词管理器，允许用户浏览、搜索、编辑和删除本地缓存的歌词文件。
- **多应用切换**：在 Android Auto 的浏览界面中，列出当前所有正在播放媒体的应用，允许用户一键切换控制焦点。
- **高度可配置性**：
    - 提供设置界面，允许用户启用/禁用歌词功能、配置 API 密钥、切换语言、管理应用过滤规则等。
    - 支持简体中文和繁体中文的自动转换。
- **权限管理**：清晰地引导用户授予必要的“通知访问”权限，并能在权限缺失时提供明确的提示。

## 3. 技术架构

应用采用现代 Android 开发技术栈，并遵循 SOLID 设计原则，以模块化的方式构建，确保了代码的清晰性、可维护性和可测试性。

- **前端**：完全使用 Jetpack Compose 构建，实现了响应式和声明式的用户界面。
- **后端/核心逻辑**：
    - **`MediaBrowserServiceCompat` (`MediaBridgeService`)**: 作为 Android Auto 与应用通信的入口点。它负责处理来自 Android Auto 的浏览请求（`onGetRoot`, `onLoadChildren`）并初始化媒体会话。
    - **`MediaBridgeSessionManager` (协调者)**: 作为应用的核心协调者，它持有并管理 `MediaSessionCompat` 的实例。它不再直接处理状态更新或歌词逻辑，而是将这些任务委托给专门的组件。
    - **`MediaStateUpdater`**: 专门负责更新 `MediaSessionCompat` 的元数据（`MediaMetadataCompat`）和播放状态（`PlaybackStateCompat`）。它包含了处理媒体信息（如过滤无效的艺术家信息）的所有逻辑。
    - **`LyricDisplayManager`**: 专门负责管理歌词的完整生命周期。它处理歌词的获取、同步启动/停止，以及将当前歌词行更新到 `MediaSessionCompat` 上。
    - **`NotificationListenerService` (`MediaNotificationListener`)**: 通过监听系统通知，实时感知其他音乐应用的播放状态变化。
    - **`MediaControllerManager`**: 用于主动扫描系统中的所有活动媒体会话，从中获取 `MediaController` 实例，进而读取精确的媒体信息和控制目标应用。
- **数据流**:
    1.  `MediaNotificationListener` 或 `MediaBridgeMediaCallback` 中的同步请求触发 `MediaInformationRetriever`。
    2.  `MediaInformationRetriever` 通过 `MediaControllerManager` 获取到当前活跃的第三方媒体应用的 `MediaController`。
    3.  从 `MediaController` 中提取出 `MediaInfo` 数据模型。
    4.  `MediaBridgeSessionManager` 接收到新的 `MediaInfo`，并将其分发给：
        a.  `MediaStateUpdater`，用于更新 Android Auto 上的媒体元数据和播放状态。
        b.  `LyricDisplayManager`，如果歌词功能开启，它将启动歌词同步流程。
    5.  `LyricDisplayManager` 内部通过 `LyricCache` 和 `LyricsManager` 获取歌词，并启动 `LyricSyncEngine`。
    6.  `LyricSyncEngine` 将同步的歌词行通过回调交给 `LyricDisplayManager`，后者负责更新 `MediaSessionCompat` 的元数据以显示歌词。
    7.  Android Auto 系统自动感知到 `MediaSessionCompat` 的变化，并更新其界面显示。
    8.  当用户在 Android Auto 上进行操作时，指令通过 `MediaBridgeMediaCallback` 转发给 `MediaControllerManager`，最终由目标应用的 `MediaController` 执行。

![Architecture Diagram](https://i.imgur.com/example.png)  *（这是一个占位符，实际应替换为更新后的架构图）*

## 4. 功能模块详解

### 4.1. 媒体桥接与控制

- **发现机制**: 通过 `MediaControllerManager` 扫描由 `MediaSessionManager.getActiveSessions()` 返回的所有活动会话。应用会过滤掉自身以及用户在设置中选择忽略的应用。
- **信息提取 (`MediaInformationRetriever`)**:
    - 从 `MediaController.getMetadata()` 中提取歌曲标题、艺术家、专辑、时长和专辑封面 (`Bitmap`)。
    - 从 `MediaController.getPlaybackState()` 中提取播放状态（播放/暂停）、当前位置（position）和支持的操作。
    - 获取并缓存源应用的包名、应用名称和应用图标。
    - **封面合成**: 为了在专辑封面上清晰地标识音乐来源，应用会将源应用的图标（如 Spotify 图标）绘制在专辑封面的右下角。
- **控制转发 (`MediaBridgeMediaCallback`)**:
    - 将标准的 `onPlay`, `onPause`, `onSkipToNext`, `onSkipToPrevious`, `onSeekTo` 指令，通过 `MediaController.getTransportControls()` 直接转发给当前活跃的源应用 `MediaController`。
    - 实现 `onFastForward` 和 `onRewind` 作为自定义快进/快退10秒的功能。

### 4.2. Android Auto 集成

- **`MediaBrowserServiceCompat` (`MediaBridgeService`)**:
    - `onGetRoot()`: 简单返回一个固定的根节点ID，允许所有 Android Auto 连接。
    - `onLoadChildren()`: 这是实现应用切换的关键。当 Android Auto 请求浏览内容时，此方法会返回一个 `MediaItem` 列表，其中每一项都代表一个当前正在播放媒体的应用。`MediaItem` 中包含了应用的名称和图标，其 `mediaId` 被设置为该应用的包名。
- **播放切换**: 当用户在 Android Auto 的列表中选择一个 `MediaItem` 时，`onPlayFromMediaId` 会被调用。应用根据传入的 `mediaId`（即包名）找到对应的 `MediaController`，并将其设为当前的控制目标，然后立即刷新媒体信息。

### 4.3. 歌词子系统

- **`LyricsManager`**: 负责从多个 `LyricsProvider`（如 `MusixmatchProvider`, `LrcApiProvider`）获取 LRC 格式的歌词文本。
- **`LyricCache`**:
    - **内存缓存**: 使用 `MutableMap` 缓存已解析的歌词列表 (`List<LyricLine>`)，避免重复解析。
    - **文件缓存**: 将从网络获取的原始 LRC 文本以 `[歌名]_[歌手].lrt` 的格式保存在应用的外部文件目录中。下次请求时，优先从文件加载。
    - **空缓存**: 如果一首歌确定没有歌词（网络请求失败且本地无缓存），会创建一个空的 `.lrt` 文件作为标记，避免对同一首歌进行不必要的重复网络请求。
- **`LyricSyncEngine`**:
    - 接收解析后的歌词列表 (`List<LyricLine>`) 和当前的播放位置 (`startPositionMs`)。
    - 使用 `Coroutine` 启动一个同步任务。它会计算出距离下一行歌词显示所需的时间，并 `delay()` 相应的时长。
    - 时间到达后，通过回调 (`onLineChanged`) 将当前行的歌词文本发送出去。
    - `LyricDisplayManager` 在收到回调后，会更新 `MediaSessionCompat` 的元数据，将歌曲标题（`METADATA_KEY_TITLE`）临时替换为当前歌词行。这是将歌词显示在 Android Auto 主屏幕上的巧妙方法。
- **`LyricsRepository`**: 提供对本地 `.lrt` 文件的增、删、改、查操作，为 `LyricsManagerScreen` 和 `LyricsEditorScreen` 提供数据支持。

### 4.4. 用户界面 (Jetpack Compose)

- **`MainActivity`**: 应用的主入口，作为导航中心，管理 `MainScreen`, `SettingsScreen`, `LyricsManagerScreen`, `LyricsEditorScreen` 之间的切换。
- **`MainScreen`**:
    - 显示当前媒体信息（来源应用、歌曲、艺术家）。
    - 提供一个醒目的横幅，在缺少“通知访问”权限时引导用户去设置中开启。
    - 提供进入设置和歌词管理器的入口。
    - 如果歌词功能已启用，提供一个“编辑当前歌词”的快捷入口。
- **`SettingsScreen`**:
    - **语言切换**: 允许用户在系统默认、英文、简体中文、繁體中文之间切换。
    - **功能开关**:
        - 启用/禁用歌词功能（带有风险提示对话框）。
        - 启用/禁用简体中文自动转换。
        - 启用/禁用“忽略原生支持Android Auto的应用”的规则。
    - **API密钥配置**: 提供文本框让用户输入 Musixmatch 和自定义 LRC API 的凭据。
- **`LyricsManagerScreen`**:
    - **列表与筛选**: 以列表形式展示所有本地缓存过的歌曲。支持按“全部”、“有歌词”、“无歌词”进行筛选，并支持通过关键词搜索歌曲或艺术家。
    - **批量操作**: 支持长按进入多选模式，对选中的歌词进行批量删除。
    - **导航**: 点击列表项可进入 `LyricsEditorScreen` 进行编辑。
- **`LyricsEditorScreen`**:
    - 提供一个全屏文本编辑器，让用户可以手动修改或创建 LRC 格式的歌词。
    - 支持保存和删除操作。

## 5. 数据模型

- **`MediaInfo`**: 核心数据模型，用于在应用内部传递完整的媒体状态。
  ```kotlin
  data class MediaInfo(
      val appPackageName: String,
      val appName: String,
      val title: String,
      val artist: String,
      val album: String,
      val duration: Long,
      val position: Long,
      val isPlaying: Boolean,
      val albumArt: Bitmap?,
      val appIcon: Bitmap?
  )
  ```
- **`LyricLine`**: 代表一行解析后的歌词。
  ```kotlin
  data class LyricLine(val timeSec: Float, val text: String)
  ```
- **`LyricsEntry`**: 用于在 `LyricsManagerScreen` 中显示的列表项。
  ```kotlin
  data class LyricsEntry(
      val key: String, // "title_artist"
      val title: String,
      val artist: String,
      val hasLyrics: Boolean
  )
  ```

## 6. 非功能性需求

- **权限**: 应用必须在启动时检查 `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` 权限，并在未授予时明确引导用户。
- **性能**:
    - 封面和图标的加载与处理应在后台线程进行，避免阻塞UI。
    - 歌词的网络请求必须在 IO 协程中执行。
    - 内存使用应被优化，特别是 `Bitmap` 对象的管理和缓存。
- **兼容性**: 应用的目标是广泛兼容主流的 Android 音乐应用。
- **健壮性**: 应妥善处理各种异常情况，如网络请求失败、权限被撤销、媒体会话突然终止等，避免应用崩溃。
