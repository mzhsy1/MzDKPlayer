# 更新日志 (Changelog)

## [未发布]

### 变更（工程结构，第二阶段：tv / phone 彻底分离）
- **新增 `:core` module**：`danmaku / data / di / player / tool / viewmodel` 全部下沉为共享业务层，里面**不含任何设计系统依赖**（既没有 `androidx.tv.material3` 也没有 `androidx.compose.material3`），也**没有 android 资源目录**。`:app` 只剩「壳 + 两套 UI」
- **`:app` 加 product flavor `tv` / `phone`**：源码分别在 `app/src/tv/`、`app/src/phone/`，清单（Activity / banner / theme）与资源各自独立；依赖按 variant 解析，因此**电视端回到 compose `1.12.1`**（不再是 1.13.0-alpha01），手机端才是 `material3 1.5.0-alpha29 + compose 1.13.0-alpha01`
- **两个 App 用不同 `applicationId`**：电视端沿用 `org.mz.mzdkplayer`（老用户原地升级、Release 资产口径不变），手机端为 `org.mz.mzdkplayer.phone`。两者可以同时装在一台设备上，因此「电视端只保留 `LEANBACK_LAUNCHER` 以防桌面双图标」的妥协不再必要
- **清掉 core → app 的反向依赖**：`Tools` 里依赖 `R.drawable` 的文件/音轨图标函数移进 tv 源集（`ui/common/FileIcons.kt`）；`VideoPlayerStatus` 不再携带 string 资源 id，文案映射进 `ui/common/VideoPlayerStatusText.kt`；`MzToastManager` 从 UI 包下沉到 `:core`；`selectedDataSourceFactory` 等数据源工厂从 UI 包移进 `player/core/BuilderMzPlayer.kt`；`MovieViewModel` 不再引用 app 的 Application 类，改用 `:core` 自己的 `di/AppContext`
- 随 module 边界收口，`FileBrowserLogic` / `SubtitleOffsetLogic` / `PhoneThemeLogic` / `PlaybackPreferenceRepository` 以及它们签名里的 `PlaybackPreference`、`PlaybackTrackRef`、`SmbUrlParts`、`HttpDirEntry` 由 `internal` 提升为 public（UI 层现在在另一个 module）
- 单测随业务层搬进 `:core`：CI / Release 工作流里的 `:app:testDebugUnitTest` 改成 `:core:testDebugUnitTest`，CI 另外 `assembleTvDebug` + `assemblePhoneDebug` 两个变体；Release 只发电视端（`assembleTvRelease`），产物名由 `app-*.apk` 变为 `app-tv-*.apk`
- 已知缺口：手机端暂未接管 `video/*` 的 VIEW intent（那是电视端 `MainActivity` 的整套路由），权限也仍是两 flavor 共用，后续按需拆分

### 新增
- **手机端支持音乐播放与图片查看（第五阶段）**：六个来源的目录列表现在会把音频与图片也认出来（行首图标分别是音符与图片），点音频进手机端音乐播放页、点图片进图片查看页；此前这两类都只弹一句「暂不支持」
- 手机端音乐播放页：读取并回写 `audio_cache` 里的内嵌标题 / 歌手 / 专辑 / 歌词 / 封面（本地封面文件与内嵌图片都支持），界面是大封面 + 滑块进度 + 上一首 / 播放暂停 / 下一首 + 底部弹出播放列表 + 可折叠歌词（按 LRC 时间轴高亮并自动跟随）。播放中每 10 秒与退出时把进度写进 `media_history`（`mediaType = AUDIO`），换歌后进度不会记到上一首上
- 手机端点音频时会把**同目录**的音频收成一张播放列表（顺序与列表页一致，地址沿用各协议列目录时算好的那个），所以自动连播与上一首 / 下一首都顺着目录走；播放列表写进 `AudioPlaylistRepository`（会落盘），播放页只从路由拿一个下标，不把整张列表塞进路由
- 手机端图片查看页：左右滑动切换同目录的图片，双指缩放（1x–6x）/ 拖动平移 / 按钮旋转 90°，放大后自动禁用翻页手势（否则平移会被翻页抢走），工具栏 4 秒无操作自动隐藏、点一下屏幕唤回。远程协议的图片靠共用的一份 `RemoteMedia` Coil Fetcher 读取
- 业务层收口两处共用实现：电视端的 LRC 解析搬进 `:core` 的 `tool/LrcParser.kt`（并补了「当前唱到第几行」`lyricIndexAt`），电视端与手机端共用；Coil 的远程图片加载器从 tv 源集移到 `app/src/main` 的 `ui/common/`，两个 flavor 共用
- `AudioViewModel` 新增 `ensureAudioCache`：进音频播放页时先确保 `audio_cache` 里有这一行，再回写元数据（`updateAudioInfo` 走的是 `UPDATE ... WHERE audioUri`，库里没有这一行会静默丢掉封面与歌词）
- 业务层新增手机端「媒体分流」纯逻辑 `PhoneMediaLogic`（媒体类型判定、音频播放列表与当前下标、图片序列的路由串拼拆），补 9 个 JVM 用例；`LrcParser` 另补 7 个。修音频元数据那一版又补了 `Id3TagReader` 15 例与元数据兜底 / 外挂歌词 9 例，单测合计 **19 类 / 366 例**
- 手机端设置页新增「**刮削与媒体库**」分组：一个「进入目录时自动刮削」总开关（手机端独有，**默认关闭**，存在 `SettingsRepository.phoneAutoScrape`），加六个来源开关（本机 / SMB / FTP / NFS / WebDAV / HTTP，与电视端共用同一份存储）。两者都打开时，进入含视频的目录才会自动联网刮削；手动点右上角「刮削本目录」不受影响
- 手机端影片详情由底部弹窗改成**独立的沉浸式页面**（路由 `phone/detail/...`）：没有 TopAppBar，剧照（无剧照时退回海报）直接铺到状态栏底下，返回键浮在图上，剧照底部渐变收进页面背景色再接白色大标题；正文是「标签胶囊（类型/年份/★评分/季集/类型标签，自动换行）→ 简介（超 4 行折叠，可展开）→ 播放 / 重新匹配 → 影片信息 → 文件信息」。未刮削时给一张说明卡和「刮削这部影片」按钮，刮完（`isScanning` 落下）页面自动回读刷新
- **手机端文件浏览接入刮削、首页改用真实数据（第四阶段）**：六个来源（本机 / SMB / FTP / NFS / WebDAV / HTTP）的目录列表都接上 TMDB 刮削 —— 视频行直接显示海报、片名与「年份 · 评分」（剧集补 `SxxExx`），顶栏出现「刮削本目录」按钮并在扫描时显示「已处理/总数」与进度条；是否自动刮削沿用「设置 → 刮削与媒体库」里按来源的那一组开关，关掉就只读缓存不联网。视频行右侧的「媒体信息」按钮弹出底部详情（海报、评分、类型、简介），既能从那里播放，也能进「重新匹配」手动搜索并覆盖这部影片的匹配结果
- 手机端把「播放地址」与「刮削结果」绑在同一个 URI 上：各协议在列目录时就把播放地址算进条目（本地 `file://`、SMB 带账号、FTP `buildFtpResourceUrl`、NFS `nfsPlaybackUri`、WebDAV 带账号目录、HTTP `resolveHttpUrl`），它同时就是 `media_cache` 的主键。此前是点击时才拼地址，容易出现「列表里显示了海报、点进播放页又变回文件名」
- 手机端首页对着电视端重做：**最近观看**（16:9 剧照 + 播放进度条 + 刮削标题）、**最近添加**（2:3 海报卡片）、**最近访问**（文件记录 + 协议名 + 播放进度），数据来自 `MediaLibraryViewModel`，与电视端共用同一份 Room 缓存；一条内容都没有时给一张引导卡直接跳到文件浏览
- 手机端新增「手动匹配」页（路由 `phone/match/...`）：用文件名解析出的关键词自动搜一次，可改关键词、切换电影/剧集、填写季集号，点结果即写回 `media_cache`
- 业务层新增 `PhoneScrapeLogic`（纯 JDK）：挑出目录里要刮的条目、跳过库里已有的、拼列表标题与副标题、算进度百分比；补 15 个 JVM 用例，单测合计 **15 类 / 326 例**
- **手机端文件管理扩展到全部来源（第三阶段）**：文件页顶部新增协议标签「本机 / SMB / FTP / NFS / WebDAV / HTTP」，除 SMB 之外的五种来源都能列目录、逐层下钻、返回上一级并把视频交给播放页（数据源标记分别是 `LOCAL / FTP / NFS / WEBDAV / HTTP`）。连接的新增与删除沿用各协议原有的 ViewModel 与本地存储（SharedPreferences），手机端只重写界面，业务层一行未改。本地文件浏览会按 Android 版本申请存储权限（Android 11 及以上走「所有文件访问」，未授权时给「去授权 / 重试」两个入口），NFS / FTP / WebDAV / HTTP 则复用一套浏览器组件：目录优先排序、返回上一级、空目录提示、失败可重试
- 手机端新增「协议浏览」纯逻辑（`PhoneFileBrowserLogic`）：本地目录上溯（不允许越过内部存储根目录）与 NFS 播放地址拼接，并补了 10 个 JVM 用例。手机端拼 NFS 播放地址时会给导出路径补前导 `/`，避免配成 `volume1/media` 这类不带斜杠的导出路径时拼出解析不出 host 的 URI
- **新增 Android 手机端入口（分支 `android-phone`，第一阶段）**：新增 `PhoneMainActivity` 与一整套 Compose **Material 3 Expressive** 手机界面（`ui/phone/`）。主题走 `MaterialExpressiveTheme` + `MotionScheme.expressive()`，组件用表达性的 `ShortNavigationBar`（底部导航）、`LargeFlexibleTopAppBar`（带副标题的伸缩标题栏）、`LoadingIndicator`（变形的多边形加载指示器）、`SegmentedButton`（主题三选一），支持「跟随系统 / 浅色 / 深色」主题切换与 Android 12+ 动态取色，主题模式存进 `SettingsRepository`。手机端只重写 UI 层，数据层、播放内核（`MzExoPlayer`）、SMB 浏览逻辑全部复用；两套设计系统零交叉引用：电视端只用 `androidx.tv.material3`，手机端只用 `androidx.compose.material3`。手机桌面入口改由 `PhoneMainActivity` 承担，电视端 `MainActivity` 只保留 `LEANBACK_LAUNCHER`，避免同一个包在手机桌面上出现两个图标。第一阶段范围：主界面与导航骨架、文件管理（SMB 连接增删 + 目录浏览）、播放验证页（只验证能否播放视频，弹幕/轨道/倍速/画面比例等面板未迁移）；`minSdk` 由 23 提升到 26
- 播放页显示刮削标题与文件日期：优先使用刮削的片名（剧集追加 SxxExx 与年份），没有刮削记录时回退文件名；日期取文件最后修改时间，覆盖本地 / SMB / FTP / NFS / WebDAV / HTTP
- 播放进度自动保存：视频与音频播放中每 10 秒落盘一次，退出播放页时再存一次，崩溃或断电最多只丢一个间隔的进度
- 设置页新增「全局画面比例」与「播放完成动作」两个条目：这两项此前只能在播放页浮层里调整，设置页没有入口；画面比例条目会标注「仅在锁定视频比例开启时生效」
- 新增「字幕时间轴」微调：以 0.5 秒为步进把字幕整体延后或提前，最多 ±30 秒，播放页浮层与「设置 → 字幕设置」两处都能调，两边共用同一个值。ExoPlayer 引擎通过包装 Media3 的字幕解析器（`SubtitleParser.Factory`）实现，VLC 引擎走原生 `spu-delay`，外挂字幕与内嵌字幕都生效；Exo 侧改完会带着新偏移量重建媒体源，并保持播放位置、播放/暂停状态以及之前选好的音轨与字幕轨。**已知限制**：ExoPlayer 引擎下只对文本字幕（SRT/ASS/SSA/VTT/TTML 等）生效，PGS、VobSub 这类图形字幕的解析结果不带绝对时间戳（时间由样本决定），偏移加不上去；VLC 引擎不受此限制。这两个入口都有对应提示文案
- 新增「记住播放偏好」开关（默认开启）：按文件记住音轨、字幕轨、倍速与画面比例，再次播放同一个文件时自动恢复；记录上限 200 个文件，超出按最近使用淘汰。开启「锁定视频比例」时画面比例不按文件记录，以全局设置为准

### 优化
- 音频元数据读取不再把整份音频下载一遍：文件头以 `ID3` 开头时，读完标签再多读 64KB 音频头就收工（`我觉得.mp3` 从约 9MB 降到约 150KB）；WAVE / MP4 这类标签可能在文件末尾的仍然读到底 —— 前者的标签就在开头，后者的封面 / 歌词挂在 `data` 或 `moov` 之后，必须读到底才能拿到
- 手机端 SMB 浏览页改用第三阶段那套公共浏览器组件：加载中 / 失败重试 / 空目录 / 返回上一级与其余五个来源完全一致（Android 17 的「本地网络权限」引导挂到公共组件的 `Blocked` 态上），顺带让 SMB 也拿到刮削展示，消掉了一份重复的 UI
- 手机端首页不再显示「第 X 阶段」说明卡与版本号（版本号在设置页「关于」里本来就有）
- 手机端 UI 依赖取 `androidx.compose.material3:material3:1.5.0-alpha29`：`MaterialExpressiveTheme` / `MotionScheme` / `LoadingIndicator` / `ButtonGroup` 这一批 Expressive API 在 1.4.0 稳定版里仍是 `internal`，只有 1.5.0-alpha 起才对应用开放。它会连带把 `androidx.compose.ui` / `foundation` / `runtime` 抬到 `1.13.0-alpha01`，而单 module 下这几个是全局解析的，所以电视端 UI 也会编译在这套 alpha core 上（电视端的源码与组件仍是 `androidx.tv.material3`，两边零交叉引用）。**该副作用已在第二阶段消除**：拆出 `:core` 并按 flavor 拆依赖后，电视端重新钉在 compose `1.12.1` 上
- 设置页分类重新整理，由 7 类调整为 9 类：新增「遥控器与交互」「刮削与媒体库」；音频/字幕首选语言分别归回音频、字幕分类；TMDB 三项与「优先加载本地 NFO」从数据源拆到刮削与媒体库，「批量扫描子文件夹层级」一并移入；遥控器上下键功能从播放分类拆出；「字幕外观」更名为「字幕设置」，「数据源与刮削」更名为「数据源」
- 文件名解析重写：支持更多季集写法（第01集 / 01. 师徒 / Season 01 Episode 01 / 1x01 / 合集 01-04 / EP01 等），自动剔除画质、音轨、发布组等噪音得到干净片名，年份取最后一个，避免「Blade.Runner.2049」这类片名被误判
- JVM 单元测试扩充到 301 个用例（13 个测试类），覆盖文件名解析、五种协议的路径拼接与上级目录、HTTP 目录页解析、播放页标题与文件日期、同名字幕匹配、弹幕解析、弹幕与 NFO 的文件路径推导、字幕时间轴偏移、播放偏好的编解码与轨道匹配、手机端主题取值、时长与语言/格式名等展示文案；为此把路径拼接、弹幕/字幕匹配、主题取值等纯逻辑集中到 `tool/` 下的工具类，页面与 ViewModel 改为委托调用（对外行为不变）
- 「数字调节」控件（`NumberControl`）支持自定义步进与显示文案，字幕时间轴的 ±0.5 秒步进复用了它
- 新增 GitHub Actions 工作流：推送到 `main` 与每个 PR 会自动编译并跑单元测试；`versionName` 变成新版本时自动构建**签名** APK 并发布 Release（发布说明取自本文件的「未发布」段落，签名库与 TMDB Key 由仓库 Secrets 注入，缺失时跳过发布而不是发出装不上的未签名包）
- 依赖升级：Kotlin 2.4.10 → 2.4.20、libvlc 3.7.5 → 3.7.6

### 修复
- 修复手机端音频**读不到内嵌封面与歌词**的问题，两处原因都改掉了：
  - `SmbUtils` 里五个 `openXxxFileInputStream` 判断「什么算音频」时要求 MIME 同时含 `audio` 与 `raw`（那是 ExoPlayer 探针给音频直通的两个词），于是普通 `audio/mpeg`、`audio/wav` 都被套上 5MB 的 `LimitedInputStream`。`看月亮爬上来 - 张杰.wav` 的 `id3 ` chunk 挂在 33MB 的 `data` 之后，被砍掉之后标题 / 封面 / 歌词全都读不出来 —— 现在只要 MIME 含 `audio` 就放开
  - jaudiotagger 另有两处盲区：WAV 的小写 `id3 ` chunk（实测它返回「无标签」），以及 `我觉得.mp3` 里 2650 字节的 `USLT` 帧（`getFirst(FieldKey.LYRICS)` 返回 null）。`:core` 新增自研的 `Id3TagReader`（v2.2/2.3/2.4、unsynchronisation、`USLT` / `APIC` / 描述符为 `USLT` 的 `TXXX`），在 jaudiotagger 的结果上做**兜底填充**（已有值不覆盖）。用真实文件的数据验证过：WAV 读出 57947 字节封面与 938 字歌词、MP3 读出 1320 字歌词
- 手机端音频支持**外挂歌词**：内嵌歌词为空时去同目录读同名 `.lrc`（`PhoneMediaLogic` 的 `lyricSiblingName` / `siblingUri` / `decodeLyricText`，按 BOM → 严格 UTF-8 → GBK 依次尝试）。实测素材里五个音频**全都**配了外挂 `.lrc`，而只有两个文件带内嵌歌词，所以它才是歌词的主要来源；老记录（`lyrics` 为 NULL，第五阶段早期留下的）会在下次进页面时补读并回写
- 手机端音频的**歌词默认展开**：原来要用户去点顶栏那个小音符图标才显示，等于「歌词没有显示」；换歌后也保持展开。歌词没有 `[mm:ss]` 时间轴时整段平铺显示，而不是什么都不显示
- 手机端音频在**没有任何标签**的文件上不再把标题写成「未知标题」：改成按「解析值（非占位）→ ID3 兜底 → 文件名解析」取值，`看月亮爬上来 - 张杰.wav` 现在显示歌名与歌手。另外 `audio_cache.localCoverPath` 用空串表示「解析过了，确实没有封面」、`null` 留给「还没解析」，手机端据此自动重解析第五阶段早期留下的记录（那时 WAV 的封面还读不出来）
- 手机端浏览列表不再显示 `.lrc` 歌词文件：它们是音频的伴生文件，点开也没有播放器，而音频页会自己按同名规则去找
- 修复手机端在 Android 17 上连接 SMB 一律超时的问题：Android 17 的「本地网络保护」会把 targetSdk ≥ 37 的应用访问局域网地址的连接在网络栈底层丢弃，表现是 `java.net.SocketTimeoutException`（而不是「连接被拒绝」），所以在手机上看就是连 445 端口超时、但服务端一切正常。手机端已声明 `ACCESS_LOCAL_NETWORK` 并在连接 SMB 前运行时申请（Android 17 起才要求，Android 16 及更早系统不受影响），未授权时给出明确提示并可重试；拒绝授权不会再白等 5 秒
- 修复画面比例与播放完成动作的文案在英文/日文/繁体环境下仍显示简体中文的问题：画面比例名从 `MzAspectRatio` 枚举里挪进多语言字串，播放页浮层与设置页共用 `ui/common/SettingOptionText.kt` 一份实现
- 修复切集后跳屏保的问题：切集动画期间新旧播放页同时存在，旧页面会把正在播放页面的屏幕常亮标志清掉，现改为引用计数管理
- 修复音频播放中换歌后进度被写到第一首记录上的问题
- 修复 NFS 从根目录进入子目录时会拼出 `//影片` 这种双斜杠路径的问题
- 修复 HTTP 目录页里出现 `mailto:` 等无法解析的链接时，整个目录列表都会加载失败的问题
- 修复 HTTP 字幕扫描会把其它目录的字幕当成本目录文件、进而拼出根本不存在地址的问题；现在只识别当前目录下的同名字幕
- 修复弹幕文件地址构造失败（例如 NFS 路径异常）会连带播放页崩溃的问题，现在只是不加载弹幕
- 统一弹幕文件与 NFO 的命名口径：视频没有扩展名时不再退化成根目录下的 `.xml` 或直接报错，而是按「同目录 + 追加后缀」处理；`/movies.v2/影片` 这类「点出现在目录名里」的路径也不会再被切错
