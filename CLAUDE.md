# 项目规范

## 沟通
- 始终用中文与用户交流

## Commit 规范
- 不添加 `Co-Authored-By` trailer
- commit 前必须先编译，确保 build 通过（`./gradlew assembleDebug`）

## UI 改动规范

做 UI 改动时须遵循以下现有逻辑：

### 布局结构
- 所有 Screen 用 `Scaffold` + `TopAppBar` 作为顶层容器
- 内容区统一用 `Column`，padding `16.dp`，`verticalArrangement = Arrangement.spacedBy(20.dp)`
- 滚动列表用 `LazyColumn`，item 间距 `spacedBy(8.dp)` 或 `spacedBy(12.dp)`

### 导航
- 每个 Screen composable 通过 `onBack: () -> Unit` 等回调参数处理导航
- 必须加 `BackHandler { onBack() }` 处理硬件返回键
- TopAppBar 返回按钮统一用 `Icons.AutoMirrored.Filled.ArrowBack`

### 组件选择
- 文字输入：`OutlinedTextField`
- 下拉选择：`ExposedDropdownMenuBox` + `OutlinedTextField`
- 开关：`Switch`（设置项）
- 主要操作：`Button`（实心）
- 次要操作：`OutlinedButton`（描边）
- 第三级操作：`TextButton`（纯文字）
- 确认/警告弹窗：`AlertDialog`，按钮用 `TextButton`
- 列表卡片：`Card`，elevation `2.dp`，圆角 `12.dp`

### 状态管理
- 本地状态用 `remember { mutableStateOf(...) }`
- 设置项变更时直接调用 `SettingsManager`，无需"保存"按钮
- Context 通过 `LocalContext.current` 获取

### 文字样式
- 区块标题：`MaterialTheme.typography.labelLarge`
- 卡片标题：`MaterialTheme.typography.titleMedium`
- 说明文字：`MaterialTheme.typography.bodyMedium`
- 次要信息：`MaterialTheme.typography.bodySmall`，颜色 `onSurfaceVariant`

### 字符串
- 所有 UI 文字必须放入 string resource，不硬编码
- 新增 string 须同步更新全部语言文件：`values/`、`values-zh/`、`values-zh-rHK/`、`values-zh-rTW/`、`values-zh-rMO/`
