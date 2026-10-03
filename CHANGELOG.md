# 更新日志 (Changelog)

## [未发布]

### 1.18.5（versionCode 116）发布说明

> 本段会被 `.github/workflows/release.yml` 取作 Release 说明，所以只放摘要；逐条细节见下方 `## [1.18.5]`。
> 本次 Release 只附带电视端 APK（`app-tv-*`）。

**新增**

- 手机端功能补齐：文件浏览、刮削、详情页、音乐与图片、视频播放页、设置页
- 刮削新增**豆瓣**数据源，与 TMDB 并存：设置里选「豆瓣优先 / TMDB 优先」（**默认豆瓣**），首选源搜不到自动用另一个兜底；豆瓣连续失败会自动短路一段时间，不影响 TMDB 兜底
- 「修改文件对应影视信息」（电视端）/「重新匹配」（手机端）现在可以选 TMDB 或豆瓣搜索，点选即按该来源写库
- 电视端五个协议连接列表视觉重做，连接表单重做，并支持编辑已有连接

**修复**

- 豆瓣海报 / 背景图「有的能加载、有的不能」：豆瓣图床按 CDN 节点分别校验 `Referer` 与 `User-Agent`，现在两者都补齐
- 电视端播放页「再按一次退出」提示有时一直不消失

**变更**

- 系统下限下调并用 flavor 分开：电视端 `minSdk` 26 → **23**（Android 6.0），手机端 26 → **24**
- TMDB 语言由「搜索语言 + 详情语言」两个设置项合成一个（搜索与详情共用，空值跟随系统）
- 手机端视频播放页交互修正：去掉与底部重复的居中按钮、长按倍速给出提示、竖屏改用 B 站式布局并精简控制栏、补上全屏按钮

---

## [1.18.5] - 2026-10-03（versionCode 116）

### 变更（TMDB 语言：两个设置项合成一个）

- 删掉「TMDB 搜索语言」，只留「TMDB 详情语言」（`SettingsRepository.tmdbSearchLang`、`SettingsViewModel.setTmdbSearchLang`、两端设置页里的那一行、四套 `strings.xml` 里的文案一并移除）。原来搜索走一条 key、详情走另一条：同一个语言要在两个地方各设一次，而在「修改文件对应影视信息」这种页面上，用户真正在意的只有点选之后抓到的详情语言 —— 只有搜索那条变了时，看起来就是「设了没用」
- 现在 `TmdbRepository` 只有一个语言入口（搜索、详情、分集、热门榜全都共用），空值仍跟随系统。老设备上残留的 `tmdb_search_lang` 是死数据，不需要迁移
- 顺带修掉「详情语言在修改文件对应影视信息里不生效」：那一页的候选列表与点选后落库的详情现在用同一套语言，不会再出现「列表中文、详情英文」这种错位

### 修复（豆瓣图床：有的图能加载、有的不能）

- 根因是**图床按 CDN 节点分别校验 `Referer` 与 `User-Agent`**：只补 `Referer` 时，OkHttp 默认的 `okhttp/x.y.z` UA 会让 `img3.doubanio.com` 这类节点直接回 403（311 字节错误页），而 `img2` / `img9` 反过来只认这种 UA。于是同一批刮削结果里「海报能出、背景图空一片」，换台设备（节点不同）表现还不一样
- 实测（同一张图，三种请求头）：不带 `Referer` → 418（14 字节占位图）；`Referer` + OkHttp UA → `img3` 403；`Referer` + 桌面 Chrome UA → 各节点均 200。所以 `MzDkPlayerApplication` 的图片拦截器现在对豆瓣域名**同时**补 `Referer` 与 `DoubanLogic.BROWSER_UA`，其它图床不受影响

### 新增（手动匹配页支持豆瓣候选）

- 电视端「纠正匹配信息」（`EditTMDBInfoScreen`）与手机端「手动匹配」（`PhoneMatchScreen`）都加了「搜索来源」开关（TMDB / 豆瓣），默认跟随设置里的「刮削首选数据源」；切开关会立刻按新来源重搜，结果列表与选中的来源永远对应
- 来源透传到 `MediaItem.source`（默认 `tmdb`），`updateMediaMapping` 据此分流：TMDB 走原来的 `TmdbRepository`，豆瓣走新增的 `DoubanScraper.fetchById()` + `MovieViewModel.updateFromDouban()`。搜索复用 `DoubanScraper.search()` —— 同一个 OkHttp 客户端、同一套请求头与同一份熔断器，只是不做「自动挑最匹配的一条」那步
- 入库口径与自动兜底一致（`source = douban`、id 借存在 `tmdbId`、`douban_tv_{id}` 分组），只有一处差异：**手动匹配时类型以详情接口的 subtype 为准**（自动流程信文件名结构，免得换个源就在电影库 / 剧集库之间跳；手动匹配是用户主动挑的条目，挑成剧集就该进剧集库并记下季集）。取详情失败时保留原记录不动，不写半条数据
- 电视端搜索按钮文案由「搜索TMDB」改为「搜索」；新增 `ui_label_search_source` / `ui_label_source_tmdb` / `ui_label_source_douban`，中 / 英 / 日 / 繁四套齐全

### 变更（minSdk：电视端 26 → 23，手机端 26 → 24）

系统下限往下放了一档，两端不再同为一个值 —— 限制来自手机端独有的依赖，电视端没有被它连累。

- **电视端 `minSdk` 由 26 降到 23**（Android 6.0）。依据是通读下来没有 26+ 的硬依赖：图标本来就是纯 PNG（`mipmap-*dpi/ic_launcher.png`，没用 `anydpi-v26` 自适应图标），代码里也没有 `NotificationChannel` 之类的 26 专属分支（`Build.VERSION_CODES` 的分支全是 `R` / `S` / `P` / `TIRAMISU`，低版本走各自 `else` 路径），本地 AAR 的下限恰好也卡在 23（`lib-decoder-ffmpeg-release.aar` 的 `minSdkVersion=23`，`akdanmaku.aar` 是 21）
- **手机端只能到 24**：Material 3 Expressive（`material3:1.5.0-alpha29`）拆分出的 `material3-ripple-android` 自己声明了 `minSdk 24`，清单合并会硬性拒绝 23。ripple 是基础组件、必然被走到，所以没有用 `tools:overrideLibrary` 硬闯（那会在 23 设备上直接 `NoSuchMethodError`）
- **落地方式是用 flavor 分开**：`app` 的 `defaultConfig` 给到 23（电视端），`phone` flavor 里覆盖成 24；`:core` 是共享库，保持 23（依赖方的下限高于被依赖方即可）。这样手机端依赖的这条限制不会把电视端一起抬上去
- 已在 23 路径上复核的兼容点：存储权限在 23 走运行时申请（电视端 `FilePermissionScreen` 与手机端 `PhoneStoragePermission` 都有 `else` 分支落到 `READ_EXTERNAL_STORAGE`）；`java.time` / `java.util.stream` 等 API 由两端都已开启的 `coreLibraryDesugaring`（`desugar_jdk_libs 2.1.5`）兜住
- 验证：`:app:assembleTvDebug` 与 `:app:assemblePhoneDebug` 均构建通过

### 新增（刮削：豆瓣数据源 + 首选源可在设置里切换）

刮削现在有两个数据源：**豆瓣**（新增）与 **TMDB**。两者始终都在用，设置里只决定**谁先谁后** —— 首选那个搜不到时，自动用另一个兜底。电视端「刮削与媒体库」与手机端同一分组各有一个「首选数据源」项（**豆瓣优先 / TMDB 优先，默认豆瓣优先**；电视端按一次换一个，手机端弹单选面板）。中 / 英 / 日 / 繁四套文案齐全。

- 默认选豆瓣的理由：中文影视在豆瓣上条目更全，而 TMDB 对冷门国产剧 / 纪录片 / 综艺要么没有条目，要么只有外文名 —— 这正是这类用户的日常内容。想要 TMDB 主源（例如更在意分集信息、评分口径与 TMDB 一致）随时可切
- 顺序判断收敛在一个纯逻辑对象里：`tool/logic/ScrapeSourcePolicy`（默认值、脏数据收敛、`order()` 只定义一次），业务侧按返回的顺序依次尝试、命中即停，不写 if-else
- 没有「完全不用某个源」的开关：两个源都会用，只是顺序不同
- **取舍写进了设置项说明**：豆瓣没有分集接口，所以豆瓣作为首选时，剧集只会落「剧集级」信息（单集标题 / 简介 / 剧照为空，详情页会隐藏「当前单集详情」卡片、背景退回剧集背景图）。这条连同「豆瓣走公开接口、连续失败会自动跳过」一起写在「首选数据源」的说明文案里（四套语言都有），用户切换前就能看到代价
- **首选源故障时会自动短路**：新增 `tool/logic/ScrapeCircuitBreaker` —— 连续 2 次网络层失败就把豆瓣短路 5 分钟（期间直接跳过、立刻走 TMDB），冷却结束后放行一次探测：通了立刻恢复，还不行就再短路一轮。目的是防止「豆瓣连不上时，每个文件都先白等一个超时才兜底」把批量刮削拖死。它以进程级静态实例挂在 `DoubanScraper` 上（`MovieViewModel` 是按页面创建的，状态放实例字段里永远攒不到阈值）
- 为了让熔断判定准确，`DoubanScraper` 把两类结局分开了：**网络层失败**（连不上 / 超时 / 非 2xx / 反爬空壳页）抛 `DoubanNetworkException` 并计数；**「接口通了但没这个条目 / 没搜到」是正常结局**，反而清零计数。`404` 单独视为「对端明确说没这个条目」，不算故障。可选步骤（背景图剧照）的失败被就地吞掉 —— 既不能连累已经刮到的信息，也不该拿它去判「源挂了」
- 单次请求超时从 15 秒收到 10 秒：豆瓣可达时响应是亚秒级，而不可达时熔断生效前的那一两次尝试就是体感的全部

**接口选型（都是实测结论，别改回去）**
- **不走「抓 HTML + 解析 JSON-LD」**：`movie.douban.com/subject/{id}/` 对自动化请求只返回约 130 字节的「载入中…」空壳页，`application/ld+json` 已经不下发了
- **搜索**用 `movie.douban.com/j/subject_suggest?q=`，返回的是**顶层 JSON 数组**（不是 TMDB 那种 `{results:[...]}`）
- **详情**用移动端 `m.douban.com/rexxar/api/v2/{movie|tv}/{id}`，**必须带 `Referer: https://m.douban.com/`**；另外电影 id 走 `/tv/` 会 404、剧集 id 走 `/movie/` 会 301，所以先按期望类型请求、失败再换另一条重试
- **UA 用桌面 Chrome**（两个接口共用一套）：它同时能过搜索与详情；不用 `api-client/1 com.douban.frodo/...` 那种伪装官方客户端的写法
- 搜索结果的 `type` 字段**不可信**（实测电视剧《三体》也返回 `type=movie`），只有 `episode` 非空能暗示「有集数」—— 所以类型只作为候选打分权重，**不做过滤**（按它过滤会把剧集全滤掉）
- 豆瓣详情没有背景图字段：**背景取 `type=W` 宽幅剧照的第一张**；海报把图床尺寸段从 `s_/m_ratio_poster` 升到 `l_ratio_poster`
- **豆瓣图床有防盗链**：不带 `Referer` 一律返回 HTTP 418（14 字节占位图）。给 Coil 配了全局 ImageLoader（`MzDkPlayerApplication : SingletonImageLoader.Factory`），只对豆瓣域名补请求头（`Referer` + 浏览器 UA，补 UA 的原因见上方「修复」一节），TMDB 等图床不受影响

**数据落库与「id 空间」隔离（本次唯一动到库结构的地方）**
- `MediaCacheEntity` 新增 `source` 列（`tmdb` / `douban`），DB 版本 8 → 9（`MIGRATION_8_9`，老数据统一回填 `tmdb`）。原因：**豆瓣条目 id 与 TMDB id 的数值区间是重叠的**，两者都写在 `tmdbId` 一列里，没有来源标记就会出现「豆瓣刮到的片子」和「TMDB 刮到的片子」被 `GROUP BY tmdbId` 并成一张卡片、选集 / 多版本串号
- 媒体库分组改为 `GROUP BY source, tmdbId`；`getMovieVersions` / `getEpisodesForSeries` 增加 `source` 参数（四个调用点同步传入实体自己的 source）
- **详情页刹车**：详情页在缓存未命中时会把 `tmdbId` 直接当 TMDB id 去联网，豆瓣记录这么做会查到另一部片子。现在电影 / 剧集两条详情路径都按 `source` 判断：非 TMDB 来源一律以本地缓存收尾（剧集的分集状态用 `Resource.Error` 收口，UI 会隐藏「当前单集详情」卡片，背景退回剧集背景图）
- 豆瓣没有分集接口，所以豆瓣来源的剧集只落「剧集级」信息（`episodeName` 等分集字段留空）；类型仍以文件名结构为准，与豆瓣 `subtype` 不一致时只记日志

**测试**：新增 `core/src/test/java/org/mz/mzdkplayer/tool/DoubanLogicTest.kt`（27 例）、`ScrapeSourcePolicyTest.kt`（4 例）、`ScrapeCircuitBreakerTest.kt`（6 例）。豆瓣的样例数据全部取自实测响应，顺带把「接口字段名没变」这件事钉住；覆盖 URL 构造 / 图床尺寸升档 / 标题与年份归一化 / 候选打分（含上面那条「`type` 不可信」的回归）/ DTO 映射 / 404 错误体 / 剧照取图；另外覆盖首选源顺序（默认豆瓣、脏数据不会算出「一个源都不查」）与熔断（没到阈值不短路、短路期内一律跳过、冷却后放行一次探测、探测失败继续短路、成功立刻恢复）。熔断器的用例把时间当参数传，不依赖真实时钟。`:core` JVM 单测合计 **28 类 475 例**（`.\gradlew.bat :core:testDebugUnitTest`）

**手动匹配（本轮已补上）**：手动匹配页原本只列 TMDB 候选 —— 自动刮削能吃豆瓣，手动纠错时却选不到豆瓣条目。现在把「来源」透传到 `MediaItem`、并在 `updateMediaMapping` 里按来源分流，两个源都能搜能选，详见上方「新增（手动匹配页支持豆瓣候选）」。

### 修复（电视端：播放页「再按一次退出」提示有时一直不消失）

视频播放页（`VideoPlayerScreen`）与音频播放页（`AudioPlayerScreen`）的返回提示会偶发地挂在画面上收不掉，电视端 `MzToast` 又没有「手动关闭」入口，只能等它自己走。两个成因都修掉了。

- **提示原本是在「组合期」发出的**：写法是 `if (showToast) { showToast(context, 文案); showToast = false }` —— 组合期副作用，`showToast()` 不是幂等的。播放页每帧都在重组（进度 / 网速 / 弹幕 / 字幕），只要复位那一次状态写没落住（组合被丢弃时写会被一并丢弃），之后每次重组都会重新 `show` 一遍；而 `MzToastState.show()` 内部是「取消上一个倒计时任务 + 重新起一个」，于是 3 秒的自动消失计时被一路重置 —— 表现为提示有时正常消失、有时一直挂着。现在改成在 `BackHandler` 的按键回调里发提示（用 `context.getString` 取文案），并按此删掉 `showToast` 这个只为副作用存在的状态
- **`MzToastState` 在作用域死亡时会卡住**：倒计时任务跑在 `MzDKPlayerAPP` 的 `rememberCoroutineScope()` 上，`MainActivity` 没有配 `android:configChanges`（任何配置变化都会重建 Activity），作用域一旦被取消，那个「等 3 秒把 `isVisible` 置 false」的协程就再也不会回来，`isVisible` 永远停在 true，新界面一进来就顶着一个消不掉的提示框。现在：① 作用域已取消时直接不弹（弹了就没人负责收）；② 倒计时的 `finally` 里兜底把自己收掉，并用一个自增 token 保证「被新提示顶掉的旧提示」不会在收尾时把新提示一起抹掉
- 新增 `core/src/test/java/org/mz/mzdkplayer/common/MzToastStateTest.kt`（5 例）：正常弹出、倒计时自动收、作用域已死时不弹、显示期间作用域被取消要收掉、新提示顶掉旧提示不被误杀。`:core` JVM 单测合计 **25 类 438 例**
- 电视模拟器实测：播放页按一次返回，提示出现在画面上；3 秒后自行消失，播放与字幕不受影响（改前该场景下提示会一直停在画面上）

### 变更（电视端：各协议连接列表界面美化）

五个协议连接列表页（`FTP` / `SMB` / `NFS` / `WebDav` / `HTTPLinkConListScreen`）连同它们共用的 `ui/screen/common/FileConListCompent.kt` 一起重做视觉。改动只在 `app/src/tv` 与四套 `strings.xml`，手机端与 `:core` 一行未动。

- **协议强调色**：新增 `ConListAccent`（SMB 天蓝 / FTP 橙 / NFS 绿 / WebDAV 紫 / HTTP 蓝）。五个协议原来共用一套灰蓝配色（图标底 `#37474F`、状态点绿色），切协议时界面长得一模一样，只有标题文字不同。强调色用在标题栏图标底、标题栏底部的渐变分隔线、列表标题左侧竖条、卡片图标底与聚焦描边 / 光晕
- **卡片会翻色了**：`myCardColor()` 的聚焦底色是米白、按下底色是深灰，而卡片里详情标签的文字色（`#B0B0B0`）与图标色是写死的浅色 → 聚焦后是白底白字，地址 / 共享目录几乎看不清。现在由 `interactionSource` 的聚焦与按下状态统一决定「深底浅字 / 浅底深字」两套配色
- **卡片**：圆角 16dp + 1dp 描边，聚焦换成 2dp 协议色描边 + 协议色光晕（原来无描边、聚焦只轻微放大）；图标底 50 → 56dp 并套上协议色；标题右侧新增协议徽标（`SMB` / `FTP` / `NFS` / `WebDAV` / `HTTP`）；详情之间补竖分隔线；高度 110 → 112dp
- **标题栏**：底色从纯 `#1E1E1E` 改为纵向渐变（`#202027 → #16161B`）并加一条协议色渐变分隔线；图标从「所有协议都用 `storage24dp`」改为各协议自己的图标，图标底是一格协议色。新增 `protocolIconRes` / `accentColor` 两个可选参数，默认值保持旧观感，本地文件页（`LocalFileTypeScreen`）不受影响
- **列表标题**：左侧加协议色竖条；右侧加一行遥控器提示「按「确定」打开 · 按「菜单」操作」—— 菜单键开操作面板这件事此前界面上没有任何提示
- **空态**：大图标 + 文案层级重做，并新增可聚焦的「添加连接」按钮（`ConnectionListEmpty` 新增 `onAddClick`）。列表为空时 LazyColumn 里没有可聚焦项，焦点只能靠方向键一路够到右上角；空态自带入口省掉这一步
- **操作面板**：圆角 18dp、面板内加分隔线、标题左侧协议色竖条 + 协议名、右侧滑入 / 滑出动画；三个条目补图标（删除 / 信息 / 关闭）。顺手修掉一个老问题：「编辑信息」的文字色写死暖白，而聚焦底色是米白 → 白底白字，聚焦到哪一项都看不见，现在按聚焦状态翻色
- **HTTP 页遮罩层位置修正**：遮罩层原本嵌在内容 `Column` 里，只能盖住标题栏以下的区域，现在提到最外层 `Box`，与其余四个页面一致
- **背景**：五页从纯 `#121212` 改为 `#141419 → #0E0E12` 纵向渐变；NFS 页此前根本没有背景色，一直浮在透明底上
- 新增文案 `ui_label_con_list_remote_hint`、`ui_label_press_back_to_close`，中 / 英 / 日 / 繁四套齐全

### 变更（电视端：输入框重做与各协议连接表单界面）

`androidx.tv.material3` 至今没有 TextField，项目里一直是自己拼的 `TvTextField`。这一轮把它按 tv-material3 的设计口径重做，并让五个协议表单页（`*ConScreen`）统一到「状态胶囊 + 表单卡片」的结构。改动全在 `app/src/tv`，手机端与 `:core` 一行未动。

**`TvTextField`（`ui/screen/common/Custom.kt`）**

- **焦点只由输入框自己持有**：外层从可聚焦的 `ClickableSurface` 换成不可聚焦的 `Surface`。旧实现外层本身是个焦点目标，于是「聚焦外框 → 按确定键 → 才进输入」要按两次，而且外层边框的高亮与内层输入框的焦点是两套状态，经常对不上
- 聚焦态：描边换成主题色并从 1dp 动画到 2dp、整体 1.02 倍缩放、底色提亮一档；未聚焦是低调的深灰底
- 新增参数：`label`（常驻字段名，输入后不消失）、`leadingIcon`、`isPassword`（密码掩码）、`isError`（红框红字）、`enabled`、`keyboardOptions`
- placeholder 与输入文字改成同层同缩进，修掉旧版 placeholder 缩进 15dp、输入文字却是 15dp + 上下 16dp 的错位
- **按键不再全吞**：只接管上下键（移动焦点）与返回键，其余一律放行。旧实现在所有按键上都返回 `true`，连外接键盘的字符键都会被吃掉
- 字号走 tv-material3 的 `bodyLarge` 兜底，调用方传来的 `textStyle` 依然优先（`merge`），不再把颜色硬编码成白色
- 删掉 `colors: ClickableSurfaceColors` 参数（它只用于旧外层的配色）——13 个文件、26 处调用点同步更新；`myTTFColor()` / `myTTFBorder()` 因此不再有调用者（文件保留未删）

**新增 `ui/screen/common/ConFormCompent.kt`**

- `ConnectionStatusPill`：色点 + 状态文字的状态胶囊，底色取状态色的 16% 透明度
- `ConnectionFormCard`：把一组输入框收进一张卡片，标题贴在卡片内顶部
- `connectionStatusColor(status)`：连接状态 → 指示色。五个表单页原本各抄了一份同样的 `when`，改口径要改五处

**五个协议表单页**

- 状态行从「一行粗体文字 + 一颗悬空色点」改成状态胶囊 —— 旧写法里状态色只体现在那颗点上，得读文字才知道是「连接中」还是「已连接」
- 输入框收进表单卡片，字段名用 `label` 常驻，字段与按钮有了分组边界
- 密码框改为掩码显示
- 左栏留白 16 → 24dp、字段间距 8 → 16dp；SMB 页三个按钮宽度统一（原先用 `fillMaxWidth(1f)`）
- **左栏加 `verticalScroll`**：TV 模拟器（1920×1080 @320dpi = 960×540dp）上跑出来才发现，加高后的表单会把底部三个按钮整个挤出屏幕，且 Column 不可滚动。真机 1080p @ xhdpi 同样是 960×540dp，一样会踩到
- 操作面板的三条文案（`连接操作` / `删除连接` / `取消`）从硬编码中文改成 `stringResource`：英文界面下原来会「Connection Actions 里的 Delete Connection 与 Edit Information 是英文、标题和取消是中文」混着显示。新增文案 `ui_label_connection_operation`，四套语言文件齐全
- **聚焦色改为白色**：描边与字段名原本取 `MaterialTheme.colorScheme.primary`，在电视深色底上偏紫；现在统一纯白（1.02 倍缩放与提亮底色保留）。错误态仍是红色
- **整体压紧一档**（960×540dp 上一屏放得下，不必再滚动，`verticalScroll` 只作兜底）：表单字段的垂直内边距 12 → 7dp、字段名与正文间距 4 → 2dp、表单卡片内边距 16 → 12dp 且字段间距 12 → 7dp、状态胶囊内边距 14/8 → 12/6dp、左栏留白 24 → 16dp 且块间距 16 → 8dp
- `TvTextField` 新增 `compact` 参数，默认值取 `label != null` —— **紧凑尺寸只作用于带字段名的表单字段**。各文件列表页与搜索页的搜索框没有 label，因此维持 16dp 的宽松高度（与重做前的观感一致）；否则这一轮的表单紧凑口径会把全 App 的输入框一起带矮
- SMB 与 WebDAV 的「测试连接 / 保存连接」由竖排两个全宽按钮改为**并排一行**（`weight(1f)` ×2），与 FTP / NFS / HTTP 三个页面原有布局对齐 —— 一个页面少占一行高度

### 新增（电视端：各协议连接列表的编辑功能）

五种协议（SMB / FTP / NFS / HTTP / WebDAV）连接列表的操作面板里，「编辑信息」此前只是个 `TODO`，现在能用了。改动只在 `app/src/tv` 与 `:core` 的连接存储层，手机端一行未动。

- **入口与流程**：卡片上按菜单键（或长按卡片）弹出操作面板 →「编辑信息」→ 跳到该协议原本的连接表单页（`*ConScreen`），字段按已有连接回填（含密码，与新增时一样明文显示）→ 改完点「保存连接」，保存成功后自动返回列表页，列表即时刷新
- **路由**：五个 `*ConScreen` 的注册从固定字符串改为可选参数 `?connId={connId}`（默认空串即新增模式），因此标题栏「添加连接」原有的 `navigate("SMBConScreen")` 一跳不用改；编辑走 `navigate("SMBConScreen?connId=$selectedId")`
- **保存前置条件**：编辑模式下只有「连接信息」（地址 / 端口 / 共享目录 / 账号 / 密码）真的变了才要求先「测试连接」成功；只改别名可直接保存 —— 否则改个名字也得重连一次。新增模式保持原样：必须先连上
- **`:core` 补齐更新能力**：`SMBConnectionRepository` 新增 `getConnectionById` / `updateConnection`，`SMBListViewModel` 与 `FTPListViewModel` 补同名方法（NFS / HTTP / WebDAV 的仓库与 ViewModel 本就已有，未动）。更新是**按 id 原地替换**并沿用仓库既有的空值兜底，因此不走 `hasDuplicateConnection`（新连接带着原 id，做重复校验只会命中它自己）
- `ConOpPanel` 新增 `onClickForEdit` 参数（原先这个字符串是写死的 TODO），「编辑信息」文案改走已有的 `ui_label_edit_information`，四个语言文件里本来就有

### 变更（手机端视频播放页交互修正）

真机回归第九阶段改动时顺手修掉的三个体验问题，只动手机端 UI（`app/src/phone`），`:`core`` 一行未改：

- **移除画面正中的播放 / 暂停按钮**：它与底部那一个完全重复，还浮在画面正中挡住内容。现在中央只留白（`Spacer`），点击会落到手势层，于是「点画面任意处收起控制栏」不受影响
- **长按倍速给出提示**：新增 `PhoneLongPressSpeedOverlay`，长按画面期间在中央浮出「2.0x 快进中」（档位取设置里的长按倍速，走现成的 `PhoneSettingsLogic.formatLongPressSpeed`），松手立刻收起 —— 2x 只靠「画面变快」根本看不出来
- **底部新增全屏按钮**：手机端播放页本身已经是沉浸全屏（无状态栏），但竖屏下 16:9 画面只占中间一条，所以这个按钮做的是**转屏**而不是隐藏系统栏 —— 点一下转横屏（`SENSOR_LANDSCAPE`，允许左右翻转），再点回到竖屏（`PORTRAIT`），离开播放页时把方向还给系统（`UNSPECIFIED`）。`PhoneMainActivity` 已带 `configChanges="orientation|screenSize|..."`，转屏不重建 Activity，播放不会被从头加载，Compose 状态也不会丢
- **竖屏改成 B 站式布局**：未全屏时画面只占顶部一块（`statusBarsPadding` 避开挖孔、高度按视频比例），下方是 `PhonePlayerVideoInfo` 信息区（海报 / 年份 / 评分 / 类型 / 连接与文件日期 / 可折叠简介）；全屏（横屏）时画面区恢复整屏、信息区不显示。所有播放器图层（画面 / 手势 / 字幕 / 弹幕 / 控制栏 / 提示条）都收进「画面区」这一个 `Box`，于是**在信息区上下滑不会再被当成调亮度 / 音量**；画面区还 `consumeWindowInsets(navigationBars)`，否则控制栏会白抬一条导航栏的高度、贴不到画面下沿
- **竖屏的控制栏精简成一行**：未全屏时只留「播放暂停 · 时间 · 全屏」，进度条与上一首 / 下一首 / 快退 / 快进只在横屏出现 —— 竖屏画面区只有约 245dp 高，原来那套（顶部栏 + 提示条 + 进度条 + 时间 + 六个按钮）几乎把画面盖满。双击左右 1/3 快进 / 快退的手势不受影响，那是手势层的事
- 信息区的配色写死不跟随主题（`Color(0xFF121214)` 一整套）：播放页是沉浸式黑底，浅色主题下若用 `MaterialTheme.colorScheme` 会变成浅底深字，与上面的画面直接撕裂
- 新增的图标 `Fullscreen` / `FullscreenExit` 按 Material 官方 24dp 路径补进 `PhoneIcons`（`material-icons-core` 里没有，为一个按钮拖进 `material-icons-extended` 不划算）；新增文案补齐了中 / 英 / 日 / 繁四套

### 变更（第九阶段：协议层单元测试，第一层）

本阶段只做「把纯逻辑从协议 IO 里抽出来 + 加测试」，不引入任何新测试依赖（没有 mockito / robolectric），也不改任何行为。

- 新增 `tool/logic/StreamSizePolicy`：`sampleMimeType` → 读取策略（音频 / 视频 / 图片放开，其余限 5MB）。这段判断原本在 `SmbUtils` 的五个 `openXxxFileInputStream` 里各抄了一份（五个方法共 20 行 if-else），现在五处都委托到这里。`UNLIMITED_RAW`（图片）之所以单独一项，是为了如实记录「FTP / HTTP 里图片是提前 return 原始流、不套关闭包装」这个历史差异，真要统一只需删掉这一项
- 新增 `tool/logic/ProtocolUriParser`：SMB 路径拆分（共享名 / 共享内路径）与 FTP 默认端口。前者原本内联在 `openSmbFileInputStream` 里，只有连上真服务器才会执行到，写错的表现是**拿着错误的 share 去连接**，而报错只有一句 `Failed to open SMB file`
- `SmbUtils.openNfsFileInputStream` 与 `NFSDataSource` 的 NFS 路径拆分改用已有的 `SidecarPathLogic.splitNfsRaw`，与 `getDanmakuNfsUri` 收敛到同一份实现
- 新增三个测试类共 36 例：`StreamSizePolicyTest`、`ProtocolUriParserTest`、`LimitedInputStreamTest`。`LimitedInputStreamTest` 顺带钉住两处与 `InputStream` 契约的既有偏差：`read(b, off, 0)` 返回 `-1` 而不是 `0`（断在测试里是为了「改口径时必须被看见」，不是认可这个行为）
- JVM 单元测试合计 **24 类 433 例**

### 已知缺口（第九阶段记录）

- **协议 IO 本身仍未覆盖**：`SmbUtils` 的五个 `openXxxFileInputStream` 与 `FTPDataSource` / `SMBDataSource` / `NFSDataSource` / `WebDavDataSource` 都要连上真服务器才跑得起来，且都被 `android.net.Uri` / `SharedPreferences` 挡住，纯 JVM 测试跑不了。下一层要么引 `MockWebServer`（HTTP / WebDAV）与 `ftpserver-core`（FTP 起本地回环），要么先给 `SMBClient` 之类的硬编码 `new` 做依赖注入再 mock
- `userInfo` → 账号密码的解析在项目里**至少 9 处**各自实现（`FileTimeParse.credentials`、`SmbUtils` 三处、`FtpDataSource`、`SubtitleScanner`、`LocalProxyServer`、`FileTimeResolver`、`WebDavDataSource`），口径互不相同 —— `FileTimeParse` 保留含冒号的密码（`a:b:c` → `a` / `b:c`），`SmbUtils` 丢掉第三段，`FtpDataSource` 只认恰好两段。收敛它必然改变某个协议的行为，故本轮未动

### 新增（第八阶段：手机端设置页与手机特色设置）

手机端设置从第一阶段那三组（外观 / 刮削 / 版本）补齐到**与电视端同一口径**，并加了一批**只有手机才有**的设置项；设置页本身也从「一页铺到底」改成手机通用的两层结构。

- **设置页改成「首页 + 二级分类」**：首页只有「外观」（主题 / 动态取色，手机上改得最勤，多一层跳转纯属添堵）与七个分类入口，点进去是独立一页（播放 / 音频 / 字幕 / 界面与首页 / 刮削与媒体库 / 工具与权限 / 关于软件），分类页里底部标签栏收起、返回箭头回到设置首页。路由是 `phone/settings/{category}`，与作为标签页的 `phone/settings` 按路径段数区分
- **补齐电视端已有的设置**（两端写的是同一份 `SettingsRepository`，所以永远是同一个值）：
  - 播放：默认播放内核（Exo / VLC）、ISO 蓝光播放行为、全局画面比例、锁定视频比例、播放完成动作、快进 / 快退时长、记住播放偏好
  - 音频：音频首选语言、音频直通、Exo 音频解码模式
  - 字幕：字幕首选语言、字幕时间轴、字号、字体颜色、背景颜色、距底部位置、强制 PGS 居中、自动加载同名字幕、第三方字体
  - 刮削与媒体库：「进目录自动刮」总开关、六个来源开关、优先加载本地 NFO、TMDB 搜索 / 详情语言、TMDB API 地址、批量扫描递归层级、移除 WebDAV 列表首项
  - 工具与权限：存储权限状态与跳转、清理影视资料库、清理音乐资料库、清空播放历史（三项都带二次确认）
  - 关于软件：版本、作者、官方网站、GitHub、Gitee、版权与免责声明（点开用系统浏览器）
- **手机特色设置（都是真的生效，不是摆设）**：
  - **左右半屏上下滑调亮度 / 音量**：左半边亮度、右半边音量，两个开关可分别关掉；滑动时中央给一个「亮度 60%」的小提示，松手 900ms 后消失
  - **长按画面临时倍速**（2x / 3x 可选）：按住时切到所选倍速，松手还原；**不会**改动浮层里那个「整段视频倍速」的值，所以长按前后倍速显示是一致的
  - **控制栏自动隐藏时长**可调（3 / 5 / 8 / 10 秒）：第七阶段这个值是写死的 5 秒，默认值仍然是 5 秒，老用户观感不变
  - **首页三个区块可分别关掉**（最近观看 / 最近添加 / 最近访问）：全部关掉时首页只剩文件与设置两张快捷卡，不会再弹「还没有内容」的引导卡（那是给真的没有数据的人看的）
- **交互按手机重做**：枚举型设置（内核、语言、画面比例、完成动作、解码模式、背景色……）改成底部弹出的单选列表，电视端那套「确定键循环切换」在手机上要按五六次才能选中最后一个；数值型设置用 ± 按钮，步进按「手指点得动」来定（快进时长 5 秒、字号 2sp、底距 10dp、字幕时间轴 0.5 秒）；TMDB 地址用系统输入法输入 + 「测试连接」并当场显示结果，保存时自动补结尾的 `/`（Retrofit 的 `baseUrl` 要求）
- **第三方字幕字体改用系统文件选择器（SAF）**：挑中的文件会**复制进应用私有目录**再存绝对路径 —— 播放页的 `Font(File)` 只认真实文件路径，而 `content://` 的读取授权出了本次会话就失效；同时提供「恢复默认字体」

### 优化

- 设置值 → 界面文案的映射（`formatLang` / `parseBgColorName` / `formatSubFontName` / `formatAppLang` / `formatAudioDecodeMode` / `formatRecursiveScanLevel` / `formatTmdbLang` / `formatIsoPlaybackMode`）从 tv 源集搬到 main 的 `ui/common/SettingValueText.kt`：手机端设置页要用同一批文案，两端各写一份迟早会漂移（与第七阶段搬 `SettingOptionText.kt` 同一个理由）
- `:core` 新增手机端设置的纯逻辑 `PhoneSettingsLogic`：档位收敛（控制栏隐藏秒数、长按倍速 —— 手改 prefs 或历史脏数据落到档位之外时回退默认）与上下滑百分比换算（按屏幕高度折算，结果收敛在 0..100；用四舍五入而非截断，否则浮点误差会把 50−30 算成 19）。补 12 个 JVM 用例，单测合计 **21 类 / 395 例**
- `SettingsRepository` 新增 8 个手机端键（左滑调亮度、右滑调音量、长按倍速开关与倍速值、控制栏隐藏秒数、首页三个区块），默认值都等于第七阶段的既有行为
- 手机端设置页去掉「其余设置项将在后续阶段迁移到手机端」的占位文案

### 已知缺口（第八阶段记录）

- **App 语言仍未开放给手机端**：电视端那套走 `LanguageManager` → `AppCompatDelegate.setApplicationLocales`，而手机端入口 `PhoneMainActivity` 是 `ComponentActivity`（不是 `AppCompatActivity`），在 Android 13 以下不会生效。要么把入口换成 AppCompat 并改用 AppCompat 主题（影响面较大），要么沿用系统设置里的「按应用设置语言」，留给后续阶段
- **视频隧道模式（`setting_tunneling`）没有搬到手机端**：它是给 HDMI 直通 / 大屏 Exo 播放器用的，手机上开着只可能出问题，没有收益
- 电视端的「遥控器上下键功能」与「隐藏详情页」不搬：手机没有遥控器；手机端的详情页也只在用户主动点「媒体信息」时才打开，不存在「点一下会不会先去详情页」这个问题

### 新增（第七阶段：手机端视频播放页）

手机端视频播放页从「只验证能不能播」补齐到**功能与电视端一致**：同一个 `IMzPlayer` 内核、同一套业务层能力，只把 UI 布局与操作逻辑按触屏重做。

- **播放内核与电视端同一口径**：TS / M2TS / MTS / M2T / ISO 强制走 VLC（Exo 处理不了传输流与蓝光原盘），其余按设置里的默认内核；`KeepScreenOnManager`（引用计数式常亮）、播放偏好记忆（音轨 / 字幕轨 / 倍速 / 比例）都直接复用电视端那套
- **画面与字幕**：视频层 + 自定义字幕层（共用 `:core` 的 `SubtitleView`，支持自定义字号 / 颜色 / 字体文件 / 底部距离 / PGS 居中，可一键显示或隐藏）+ 弹幕层
- **弹幕**：进页面读同目录同名 `.xml` 自动加载，设置面板里可调开关、显示区域（1/2～1/12 / 全屏）、按类型过滤（滚动 / 底部 / 顶部 / 彩色）、字号与透明度，改完当场生效并写回本地；配置拼装与推送走 `:core` 里与电视端共用的实现
- **轨道与字幕**：视频轨（清晰度 / 码率 / 编码）、音轨（语言 / 码率 / 声道 / 格式 / 采样率）、字幕轨（内嵌 / 外挂标记、关闭字幕）、VLC 下的 ISO 标题；「加载外部字幕」按同名 `.ass/.srt/.ssa/.vtt` 四个候选批量加
- **其余面板**：字幕时间轴偏移（±0.5 秒步进，与「设置 → 字幕设置」共用一个值）、播放倍速（音频直通时提示不可改）、画面比例 + 锁定全局比例、播放列表、播放完成动作（循环 / 暂停 / 播放下一个）
- **播放进度落盘**：播放中每 10 秒 + 退出播放页时各写一次 `media_history`（`mediaType = VIDEO`，带上连接名），首页「最近观看」对视频不再是空的；进入时若上次看到 5 秒以上，先跳到那个位置并给一条「您上次看到 xx:xx，点击此处从头播放」
- **触屏交互**（电视端那套 KeyEvent + 焦点体系在手机上不成立，全部重做）：
  - 单击画面显隐控制栏，播放中 5 秒无操作自动收起（暂停时不收起）；
  - 进度条换成可拖动的 `Slider`（电视端是左右键累计 + 落定回调），拖拽时时间跟着手指走；
  - 双击屏幕左右各 1/3 快退 / 快进，中央给一个 700ms 的箭头提示（中间 1/3 不响应，避免误触跳进度）；
  - 所有面板收进底部弹出的 `ModalBottomSheet`，子面板的返回键回到根面板（对应电视端的抽屉层级）；
  - 全屏沉浸：进页面隐藏状态栏与导航栏（从边缘滑动可临时唤出），退出时还给系统；
  - 返回键交给导航默认行为 —— 电视端「按两次退出」是为了防遥控器误触，手机不需要。
- 手机端六个来源的目录浏览页在点视频时会把「同目录的视频」写进 `VideoPlaylistRepository`（与音频播放列表、图片序列同一口径），播放页据此显示播放列表、上一集 / 下一集与「播放下一个」
- 播放路由补上 `connectionName`，并把 `title` 参数改成**原始文件名**（此前有的入口塞的是刮削后的展示标题，写进播放历史会变成片名）；展示标题统一由播放页按 `media_cache` 拼

### 优化

- `:core` 新增手机端播放页的纯逻辑 `PhonePlayerLogic`（强制 VLC 判定、连播下标、快进快退收敛、进度换算、继续播放阈值、倍速档位），补 11 个 JVM 用例；单测合计 **20 类 / 383 例**
- 弹幕那一段「设置 → 渲染器配置」下沉到 `:core` 的 `danmaku/DanmakuRendering.kt`（`danmakuConfigFor` / `pushDanmakuConfig` / `toDanmakuItems`）：电视端播放页与弹幕面板、手机端播放页与弹幕面板现在共用同一份拼装、推送与 XML 映射，避免两头各写一遍后「手机上改字号没反应」这类漂移
- `SettingOptionText.kt`（画面比例与播放完成动作的文案）从 tv 源集移到 `main`：手机端播放页的面板复用同一批字串，不再各写一份
- 手机端播放页的自定义字幕样式、弹幕加载、网速统计都收在播放页自己身上，不额外引入 ViewModel；弹幕 XML 的读取放到 IO 线程（电视端是在主线程直接开流）

### 修复

- **手机端播放页现在会写 `media_history`**，修掉第六阶段记录的那个缺口（首页「最近观看」对视频恒为空）
- 手机端路由的空值占位符 `~` 此前从未生效：它是**先被 Base64 编码**再和 `~` 比较的，所以解出来仍是 `~`，本机文件的连接名会一路带着 `~` 写进 `media_cache` 与播放历史；现在先解码再比，空连接名还原成空串
- 电视端几处硬编码中文改用多语言字串：播放列表 / 无内容 / 已是最后一个视频 / 「音频直通模式下不可更改倍速」

### 已知缺口（第七阶段记录）

- 手机端「设置」页仍是第一阶段那几项，**没有**内核选择（Exo / VLC）、音频直通、字幕外观等条目；因此手机端只有在遇到 TS / ISO 这类强制格式时才会用上 VLC。这些设置项与「设置页迁移」一起留给后续阶段。

### 变更（工程结构，第六阶段：代码整理）

本阶段只做「搬家与合并」，不改任何行为（编译通过 + 372 个单元测试全绿）。

- **`:core` 的 `tool/` 按职责拆包**（41 个文件，只改包名与 import）：
  - `data/datasource/`：四种协议的 `DataSource` + `WebDavHttpClient` + `SmbUtils` + `IOTools` —— 协议 IO 不再和纯逻辑混在一个目录
  - `tool/logic/`：`FileBrowserLogic`、`SidecarPathLogic`、`SubtitleMatchLogic`、`SubtitleOffsetLogic`、`PlaybackPreferenceLogic`、`FileTimeParse`、`PlayerMediaText` 与手机端的四个 `Phone*Logic` —— 都是「只依赖 JDK、可单测」的纯逻辑
  - `tool/metadata/`：音频/视频元数据与文件名解析（`AudioFileInfo`、`Id3TagReader`、`LrcParser`、`MediaInfoExtractorFormFileName`、`NfoReader` 等）
  - `tool/server/`：内置 NanoHTTPD 那几个服务（本地代理、手机扫码遥控）
  - `common/`（新包）：原先混在工具包里的 UI / 平台工具（Compose 的 `Modifier` 扩展、自定义字幕 View、封面色提取、Toast、屏幕常亮、应用语言）；`viewModelWithFactory` 归入 `di/`
  - `player/exo/PlayerMediaSources.kt`：原 `player/core/BuilderMzPlayer.kt`（内容是「按协议选数据源工厂 + 推同名字幕 + 规范化播放地址」，与 ExoPlayer 强相关），归入 Exo 包并清掉一批误导性的未使用 import
- **手机端浏览页抽公共骨架**：新增 `ui/phone/component/PhoneBrowserPage.kt`，六个来源（本机 / SMB / FTP / NFS / WebDAV / HTTP）的目录浏览现在都只写「差异声明 + 一个加载实现 + 条目映射」，重复的加载流程、状态机、失败重试、刮削会话与渲染全部共用；SMB 的「本地网络权限」与 本机的「所有文件访问」仍各有自己的引导按钮
- 手机端 UI 组件收拢到 `ui/phone/component/`：统一了「加载中 / 空 / 失败 / 缺权限」四套提示块（此前每个页面各写一遍）、海报缩略图；共享模型（`PhoneBrowserEntry` / `PhoneBrowserState` / `PhoneFileProtocol`）独立到 `ui/phone/model/`
- **`PhoneApp.kt` 从 617 行拆成四个文件**：`PhoneApp.kt`（主题 / 系统栏 / 底部标签栏）、`PhoneViewModels.kt`（16 个 ViewModel 的装配）、`PhoneNavGraph.kt`（导航图与路由参数解码）、`PhoneScrapeSources.kt`（「哪个来源参与刮削」的映射口径）。行为不变：ViewModel 仍全部挂在 Activity 作用域，路由与参数编码规则一字未改
- 手机端几处纯逻辑（SMB 上一级目录、HTTP 起始目录 URL、歌词伴生文件判定、发布日期取年份）下沉到 `:core` 并补了 6 个单元测试，单测合计 **19 类 / 372 例**；UI 里的媒体类型判定改为直接复用 `PhoneMediaLogic`，消掉了两套并行口径
- 清掉一批确认无人引用的死代码：`:core` 的 `FileMediaInfo.kt`（`setupPlayer` / `builderPlayer` 全库无调用）、`SmbMediaInfoExtractor.kt`、整文件早已被注释掉的 `ReadFlacHeader.kt`，以及 `MzTrackModels` 里没人使用的 `MzTrackType`；电视端删掉无人引用的 `AudioPlayerMainFrame` / `AudioPlayerMediaTitle` / `AudioPlayerOverlay` 三个组件，与从未被调用的 `MzDKPlayerTheme`（连带只服务于它的 `Type.kt` 与 `Color.kt` 里 6 个 Material 示例色）
- 记录（本次整理未改动行为）：手机端视频播放页不写 `media_history`，因此首页「最近观看」对视频恒为空 —— 音频播放页是写的，代码里已留 TODO 说明接法

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
