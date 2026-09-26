# MzDKPlayer - 安卓TV本地弹幕音视频播放器

![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/mzhsy1/MzDKPlayer/total)
![License](https://img.shields.io/badge/license-GPL--3.0-blue)

[English](README_en.md) | 中文

> GitHub https://github.com/mzhsy1/MzDKPlayer ｜ Gitee 镜像 https://gitee.com/mzhsy/MzDKPlayer ｜ 官网 https://mzdkplayer.pages.dev/

> MzDKPlayer 是一款专为安卓电视（Android TV）设计的本地音乐与视频播放器，支持弹幕功能、多种网络协议播放及音频视频格式播放。

---

## 目录

- [功能特性](#功能特性)
- [格式支持](#格式支持)
- [应用演示](#应用演示)
- [快速开始](#快速开始)
  - [方式一：下载安装 APK](#方式一下载安装-apk)
  - [方式二：从源码构建](#方式二从源码构建)
  - [构建常见问题](#构建常见问题)
- [使用示例](#使用示例)
- [遥控器按键](#遥控器按键)
- [技术架构](#技术架构)
- [开发指南](#开发指南)
- [硬件要求](#硬件要求)
- [项目状态](#项目状态)
- [贡献指南](#贡献指南)
- [免责声明](#免责声明)
- [许可证](#许可证)

---

## 功能特性

### 核心功能

- 🎬 **视频播放** - 支持多种视频格式的本地与网络协议播放
- 🎵 **音频播放** - 支持多种音频格式的本地与网络协议播放，歌词专辑封面显示与音乐信息，播放列表等常见功能
- 🖼️ **图片查看** - 支持多种图片格式的本地与网络协议查看
- 🏡 **媒体库** - 包含电影/电视剧/音乐库，从 TMDB 获取电影电视剧信息，支持批量添加
- 🕛 **历史记录** - 播放历史记录，包含音视频
- 🔍 **搜索功能** - 搜索电影/电视剧
- 💬 **弹幕功能** - 支持 B 站风格弹幕显示与自定义
- ⚙️ **设置功能** - 较为详尽的应用与播放设置
- 🌐 **网络协议支持**：
  - ✅ SMB 协议（当前已支持）
  - ✅ FTP 协议（当前已支持）
  - ✅ WebDAV 协议（当前已支持，其中如飞牛 NAS 提供的局域网 WebDAV 服务只支持 http，不支持 https，其他公网如阿里云正常支持）
  - ✅ NFS 协议（当前已支持）
  - ✅ HTTP 协议（当前已支持 NGINX 服务器）
- 🎚️ **多轨道选择** - 支持音轨、视频轨、字幕轨切换，倍数功能，音频软硬解码
- 🔤 **字幕自定义** - 纯文本字幕（ExoPlayer）支持自定义字体、字号、颜色、背景色与底部边距
- 🔤 **字幕时间轴微调** - 字幕整体提前或延后（0.5 秒步进，最多 ±30 秒）；ExoPlayer 引擎下仅文本字幕生效，PGS 等图形字幕暂不支持
- 🧠 **记住播放偏好** - 按文件记住音轨、字幕轨、倍速与画面比例，再次播放同一个文件时自动恢复
- 📱 **手机扫码遥控** - 应用内可开启局域网遥控页，手机扫码即可当遥控器用
- 🌏 **多语言界面** - 简体中文 / English / 日本語 / 繁體中文

### 🔊 播放进阶：音频直通 (Passthrough) 说明

MzDKPlayer 支持音频直通功能，可以将原始音频信号（源码）直接输出至功放、回音壁或支持多声道解码的电视，以获得影院级的听觉体验。

* **路径**：`设置` -> `音频设置` -> `音频透传 (Passthrough)`
* **适用范围**：**此开关仅对 VLC 播放引擎生效**。ExoPlayer 引擎会根据您的设备自动智能判断，无需手动干预。
* **使用建议**：
  * **默认状态**：建议保持 **关闭 (Off)**。ExoPlayer 已能满足大部分设备的自动适配需求。
  * **开启前提**：仅当您拥有外接功放或高端音频解码设备，且确定其支持所播放视频的音频编码格式（如 DTS-HD, TrueHD）时再开启。
  * **故障排查**：若开启后播放时遇到**无声**情况，说明您的音频设备不支持当前视频的音轨格式（例如某些电视不支持 TrueHD 直通）。**此时请务必关闭该开关**，让播放器通过软件解码转换为 PCM 输出。

---

## 格式支持

### 📺 视频格式 (Video)

* **常用封装**：MP4, MKV, MOV, AVI, WMV, FLV, WebM
* **蓝光/专业格式**：**ISO (蓝光原盘镜像)**, **M2TS**, **MTS**, TS, VOB
* **视频编码**：H.264 (AVC), **H.265 (HEVC)**, **AV1**, VP9, MPEG-2
* **特性支持**：4K/8K 超高清播放、HDR10/HLG、杜比视界 (Dolby Vision)

### 🎵 音频格式 (Audio)

* **无损/高保真**：**FLAC**, WAV, ALAC (Apple Lossless)
* **通用格式**：MP3, AAC, OGG, Opus, WMA
* **影院级音轨**：**DTS**, **DTS-HD**, **TrueHD**, AC3 (Dolby Digital), E-AC3

### 🖼️ 图片格式 (Image)

* **标准格式**：JPEG (JPG), PNG, WebP, BMP
* **现代格式**：HEIC / HEIF
* *注：暂不支持 Apple Live Photo。*

### 💬 字幕支持 (Subtitles)

* **外挂字幕**：**SRT**, **ASS**, **SSA**, VTT
* **内嵌字幕**：MKV 内嵌字幕、**PGS (蓝光原盘字幕)**、DVB、Teletext

---

> ⚠️ **注意**：TMDB 在国内可能需要代理或修改 Hosts 才能稳定访问，也可在 `设置 -> 刮削与媒体库 -> TMDB API 地址 (镜像)` 中填写镜像地址。

> 💡 小提示1：如果经常使用，建议在电视系统里把本播放器设为默认视频播放器，体验更顺滑。

> 💡 小提示2：如果设备性能不足，播放 70、80G 的原盘视频时开启弹幕可能会造成播放卡顿。

> 💡 小提示3：如果遇到使用 ExoPlayer（默认）播放器无法正常播放，可以尝试在 `设置 -> 播放与视频 -> 默认播放器内核` 选择 VLC 播放器。

---

## 应用演示

### 主界面与文件列表

![主界面截图](screenshots/Screenshot_20260708_114700.webp)
![主界面截图](screenshots/Screenshot_20260708_105320.webp)
![主界面截图](screenshots/Screenshot_20251116_163213.webp)
![主界面截图](screenshots/Screenshot_20251223_174613.webp)
![主界面截图](screenshots/Screenshot_20260708_111419.webp)

### 播放界面与弹幕效果

![视频播放界面截图](screenshots/Screenshot_20251104_190350.webp)  
![视频播放界面截图](screenshots/Screenshot_20251104_190409.webp)
![音频播放界面截图](screenshots/Screenshot_20260126_182132.webp)

### 电影/电视剧详情页面

![电影详情页面截图](screenshots/Screenshot_20251220_112824.webp)
![电视剧详情页面截图](screenshots/Screenshot_20251220_112844.webp)

### 设置页面

![设置页面截图](screenshots/Screenshot_20260708_105422.webp)

---

## 快速开始

### 方式一：下载安装 APK

> 适合只想在电视上使用的用户，不需要任何开发环境。

1. 打开 [Releases](https://github.com/mzhsy1/MzDKPlayer/releases) 页面，下载最新版本的 APK（由 GitHub Actions 自动构建）。
2. 根据电视芯片架构选择安装包（**不确定就先用通用包**）：

   | 安装包 | 适用设备 |
   | --- | --- |
   | `arm64-v8a` | 绝大多数近几年的电视盒子 / 电视（晶晨 S905X3/S928X、MT9653 等） |
   | `armeabi-v7a` | 较老的低配盒子（如 S905L 等 32 位系统） |
   | `universal`（通用包） | 体积最大，兼容以上全部架构 |

3. 把 APK 传到电视上安装（U 盘 / 当贝市场 / 电视自带文件管理器均可）。

如果通过电脑安装，推荐用 ADB：

```bash
# 电视上先开启「开发者选项 -> USB 调试 / 网络调试」
adb connect 192.168.1.100:5555        # 换成电视的实际 IP
adb install -r app-arm64-v8a-release.apk
```

### 方式二：从源码构建

#### 1. 环境要求

| 项目 | 要求 | 说明 |
| --- | --- | --- |
| JDK | **17 或更高** | 源码与目标字节码均为 Java 17 |
| Kotlin 工具链 | JDK 21 | 项目声明了 `jvmToolchain(21)`，本机没有时 Gradle 会自动下载 |
| Android SDK | **compileSdk 37** | 需要装 `Android SDK Platform 37` 与 `Build-Tools` |
| Gradle | 9.7.1 | 用仓库自带的 `gradlew` 即可，无需单独安装 |
| Android Studio | 最新稳定版 | 可选，命令行也能完整构建 |

#### 2. 克隆项目

```bash
git clone https://github.com/mzhsy1/MzDKPlayer.git
cd MzDKPlayer
```

#### 3. 配置 `local.properties`

在项目根目录新建 `local.properties`（**此文件已在 `.gitignore` 中，不会入库**），填写 SDK 路径与 TMDB API Key：

```properties
sdk.dir=D:\\Android\\Sdk
TMDB_API_KEY=你的TMDB_API_KEY
```

> ⚠️ **`TMDB_API_KEY` 是必填项**。它会被写进 `BuildConfig.TMDB_API_KEY`，缺失时项目仍能编译，但媒体库的 TMDB 刮削功能会失效。
> API Key 可在 [TMDB 开发者设置](https://www.themoviedb.org/settings/api) 免费申请，并在设置页将 API 地址指向可访问的镜像。

#### 4. 构建

```bash
# Windows 用 gradlew.bat，macOS / Linux 用 ./gradlew

# 调试包（用 debug 签名，直接能装，日常自测用这个）
./gradlew :app:assembleTvDebug        # 电视端
./gradlew :app:assemblePhoneDebug     # 手机端

# 发布包（开启混淆与资源压缩，产物未签名）
./gradlew :app:assembleTvRelease

# 只做编译校验，速度最快
./gradlew :app:compileTvDebugKotlin   # 电视端；手机端把 Tv 换成 Phone
```

> 电视端与手机端是同一 module 下的两个 **product flavor**（`tv` / `phone`），
> 源码分别在 `app/src/tv/`、`app/src/phone/`，业务层在独立的 `:core` module。
> 它们的 `applicationId` 不同（`org.mz.mzdkplayer` / `org.mz.mzdkplayer.phone`），可以同时装在一台设备上。

产物路径：

| 命令 | 产物位置 |
| --- | --- |
| `assembleTvDebug` / `assemblePhoneDebug` | `app/build/outputs/apk/tv/debug/app-tv-debug.apk`、`app/build/outputs/apk/phone/debug/app-phone-debug.apk` |
| `assembleTvRelease` | `app/build/outputs/apk/tv/release/app-tv-<abi>-release.apk`、`app-tv-universal-release.apk` |

> 发布包默认**不签名**，上架或分发前需要用 `apksigner` / Android Studio 的 *Generate Signed Bundle or APK* 补上签名。
> 项目开启了 ABI 拆分（`armeabi-v7a`、`arm64-v8a` + 通用包），所以 release 目录下会有多个 APK。

#### 5. 安装到电视

```bash
adb connect 192.168.1.100:5555
adb install -r app/build/outputs/apk/tv/debug/app-tv-debug.apk
```

#### 6. 跑一遍单元测试（确认环境没问题）

```bash
./gradlew :core:testDebugUnitTest
```

> 单元测试全部跟着业务层放在 `:core`，跑一次 `:core:testDebugUnitTest` 即可覆盖两端。

### 构建常见问题

| 现象 | 原因与解决办法 |
| --- | --- |
| `Unable to delete directory ... a process has files open` | Android Studio 正开着并占用了 `app/build` 产物。关掉 Studio 再跑命令行，**不要手动删 `app/build`** |
| 找不到 `TMDB_API_KEY` / 刮削无效 | `local.properties` 缺失或键名写错，注意不要写成 `TMDB_KEY` |
| `SDK location not found` | `local.properties` 中的 `sdk.dir` 没配，或在 Android Studio 里设置 SDK 路径 |
| Kotlin 工具链下载失败 | `jvmToolchain(21)` 需要联网下载 JDK 21；可先在本机安装 JDK 21 让 Gradle 直接复用 |
| 依赖拉取超时 | `settings.gradle.kts` 已配置阿里云/腾讯云镜像，可自行调整仓库顺序 |
| `fileHashes.lock 拒绝访问` | 先 `./gradlew --stop` 确认守护进程已退出，再删除 `.gradle/9.7.1/fileHashes/fileHashes.lock` |

---

## 使用示例

### 示例 1：播放本地文件

1. 首次启动会请求存储权限，在 Android 11+ 上选择 **「允许管理所有文件」** 体验最好（否则部分目录扫描不到）。
2. 首页 / 文件浏览 -> 选择分类（视频 / 音频 / 图片）。
3. 进入目录后点击文件即可播放；播放器会自动加载同目录下的同名 `.xml` 弹幕文件与同名字幕文件。

### 示例 2：添加网络存储（SMB / FTP / WebDAV / NFS / HTTP）

1. 侧边栏进入 **「网络存储」**，选择协议类型。
2. 点击右上角 **「新建连接」**，填写地址、账号、密码（SMB 需填共享名，FTP 需填端口）。
3. 保存后回到连接列表即可看到该存储，点进去像本地目录一样浏览、播放。
4. 支持的 URI 形态（用于排查问题时参考）：

   | 协议 | 形态示例 |
   | --- | --- |
   | SMB | `smb://user:pass@host/share/path` |
   | FTP | `ftp://user:pass@host:21/path` |
   | WebDAV | `https://user:pass@host/path` |
   | NFS | `nfs://host:/export:path` |
   | HTTP | `http://host/path`（NGINX 目录列表） |
   | 本地 | `file:///storage/emulated/0/Movies/a.mkv` |

> 所有网络协议连接都设置了超时，网络异常时会「软降级」，不会导致应用卡死或闪退。

### 示例 3：弹幕

- 弹幕文件为 B 站格式的 `.xml`，与视频放在同一目录、**文件名相同**即可自动加载。
- 播放时按遥控器 **上键** 打开弹幕设置，可调整字号、速度、透明度、显示区域等。

### 示例 4：TMDB 刮削与本地 NFO

- 电影 / 电视剧详情页的信息来自 TMDB，需要可用的网络与 API Key。
- 若媒体目录下存在同名 `.nfo` 文件，可在 `设置 -> 刮削与媒体库 -> 优先加载本地 NFO 文件` 打开优先读取，避免联网。
- 刮削结果会缓存到本地数据库，播放页标题会优先显示刮削片名（剧集自动追加 `SxxExx` 与年份），无刮削记录时回退文件名。
- 批量扫描的递归层级可在 `设置 -> 刮削与媒体库 -> 批量扫描子文件夹层级` 调整。

### 示例 5：字幕

- 播放时按遥控器 **菜单键** 调出控制栏，进入字幕菜单切换内外挂字幕轨。
- 打开 `设置 -> 字幕设置 -> 自动加载同名字幕` 后，播放时会自动扫描视频所在目录并加载同名字幕。
- 纯文本字幕（SRT/ASS 经 ExoPlayer 渲染）支持自定义字体、字号、颜色、背景色与底部边距；字体可从本地文件夹中挑选。
- 字幕与画面不同步时，用播放页浮层里的「字幕时间轴」按 0.5 秒为步进整体提前或延后（最多 ±30 秒）；`设置 -> 字幕设置 -> 字幕时间轴` 是同一个值的另一个入口。注意 ExoPlayer 引擎下只对文本字幕生效，PGS 等图形字幕请改用 VLC 引擎。

### 示例 6：手机扫码遥控

1. 打开界面上的遥控入口，应用会在局域网内启动一个轻量 HTTP 服务并显示二维码。
2. 手机连同一个 Wi-Fi，扫码打开网页即可当作遥控器使用。

### 示例 7：播放列表与历史

- 视频 / 音频播放页支持播放列表与播放完成后的动作（如自动切下一集）。
- 播放进度自动保存（每 10 秒落盘一次，退出播放页时再存一次），在 **历史记录** 中可继续上次进度。

---

## 遥控器按键

| 按键 | 播放页功能 |
| --- | --- |
| 左右键 | 快进 / 快退（长按可连续快进快退） |
| 确认键 | 暂停 / 播放 |
| 菜单键 | 显示 / 隐藏控制栏（选轨、字幕、倍速、弹幕等） |
| 上键 | 弹幕设置（可在设置中改为其他功能） |
| 下键 | 音轨选择（可在设置中改为其他功能） |
| 返回键 | 退出播放 / 收起控制栏 |

> 上键与下键的行为可以在 `设置 -> 遥控器与交互 -> 遥控器上键功能 / 遥控器下键功能` 中自定义。

---

## 技术架构

### 主要技术栈

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Kotlin | 2.4.20 |
| 构建 | Gradle / AGP / KSP | 9.7.1 / 9.4.0 / 2.3.9 |
| 界面 | Jetpack Compose for TV（`tv-foundation` / `tv-material`）+ Navigation Compose | Compose BOM 2026.09.00 / Navigation 2.9.8 |
| 播放内核 | Media3 ExoPlayer + libVLC（双引擎，统一抽象层） | 1.11.1 / 3.7.6 |
| 弹幕 | AKDanmaku + libGDX / Ashley | — |
| 数据库 | Room | 2.8.5 |
| 偏好存储 | DataStore Preferences | 1.2.1 |
| 网络 | smbj (SMB) / commons-net (FTP) / sardine (WebDAV) / nfs-client (NFS) / OkHttp | — |
| 图片加载 | Coil 3 | 3.6.2 |
| 内置服务 | NanoHTTPD（本地代理 + 手机遥控页）、ZXing（二维码） | — |
| 元数据 | Retrofit + Gson（TMDB）、jaudiotagger（音频标签） | — |

### 模块结构

工程分成一个业务层 module（`:core`）+ 一个带两个 product flavor 的应用 module（`:app`）：

```
core/src/main/java/org/mz/mzdkplayer/     # :core —— 业务层，tv / phone 共用
├── danmaku/          # 弹幕解析
├── data/
│   ├── api/          # TMDB 接口（Retrofit）
│   ├── local/        # Room：AppDatabase、MediaCacheEntity、AudioCacheEntity、MediaHistoryEntity
│   ├── model/        # 数据模型
│   └── repository/   # 仓库层，屏蔽数据来源
├── di/               # RepositoryProvider（以 viewModelWithFactory 注入 DAO）、AppContext
├── player/
│   ├── core/         # IMzPlayer 抽象、轨道模型、数据源工厂
│   ├── exo/          # MzExoPlayer 实现
│   └── vlc/          # MzVlcPlayer 实现
├── tool/             # 工具层：协议 DataSource、字幕扫描、时间解析、局域网代理与遥控服务
└── viewmodel/        # 各页面的 ViewModel

app/src/main/java/org/mz/mzdkplayer/
└── MzDkPlayerApplication.kt              # 两个 flavor 共用的 Application

app/src/tv/java/org/mz/mzdkplayer/        # 电视端 flavor（只用 androidx.tv.material3）
├── MainActivity.kt / LaunchScreen.kt     # LEANBACK_LAUNCHER 入口
└── ui/
    ├── MzDKPlayerAPP.kt      # 导航图与主框架
    ├── videoplayer/          # 播放页（components/ 下为控件、标题、弹层）
    ├── audioplayer/          # 音频播放页
    ├── picviewer/            # 图片查看
    ├── screen/               # 各页面：filehome / localfile / smbfile / ftp / webdavfile / nfs /
    │                         #        httplink / library / movie / tv / history / search / setting
    └── theme/                # 电视端主题

app/src/phone/java/org/mz/mzdkplayer/ui/phone/   # 手机端 flavor（只用 androidx.compose.material3）
├── PhoneMainActivity.kt    # LAUNCHER 入口（applicationId = org.mz.mzdkplayer.phone）
├── PhoneApp.kt             # 底部导航 + 导航图
├── PhoneTheme.kt / PhoneIcons.kt / PhoneRoutes.kt
└── screen/                 # 首页 / 文件 / SMB 浏览 / 播放 / 设置
```

两套设计系统**零交叉**：`:core` 不依赖任何 Material 库，电视端只引 `androidx.tv.material3`（compose 1.12.x），
手机端只引 `androidx.compose.material3` 1.5.0-alpha（会带进 compose 1.13.0-alpha），依赖按 variant 解析、互不污染。

`core/src/test/java/org/mz/mzdkplayer/` 下为纯 JVM 单元测试。

### 播放器双引擎

播放逻辑通过 `player/core/IMzPlayer.kt` 抽象，`MzExoPlayer`（默认，硬件解码友好、启动快）与 `MzVlcPlayer`（格式兼容性更广、支持直通）各自实现。上层界面只依赖接口，因此可以在 `设置 -> 播放与视频 -> 默认播放器内核` 中随时切换，也便于继续接入新的内核。

### 核心组件

- `VideoPlayerScreen` - 主播放器界面
- `BuilderMzPlayer` - 播放器构建与配置
- `AkDanmakuPlayer` - 弹幕播放组件
- `MovieDetailsScreen` / `TVSeriesDetailsScreen` - 电影/电视剧详情页面
- `FullDescriptionDialog` - 详细简介弹窗
- `LocalProxyServer` / `ProxyManager` - 本地 HTTP 代理，解决部分协议与 ExoPlayer 的兼容问题
- `RemoteInputServer` / `RemoteInputQRPanel` - 手机扫码遥控

### 数据层要点

- Room 数据库 `AppDatabase` 包含三张表：`media_cache`（影视刮削缓存，主键为 `videoUri`）、音频缓存、播放历史。
- 列表页传给播放页的 URI 与 `media_cache.videoUri` 保持一致，播放页可直接按 URI 命中刮削缓存，无需重复联网。
- 导航参数（URI、文件名、连接名等）统一走 Base64 编码，避免路径中的特殊字符破坏路由。

---

## 开发指南

### 常用命令

```bash
./gradlew :app:compileTvDebugKotlin          # 只编译，最快的语法校验
./gradlew :app:assembleTvDebug               # 打调试包
./gradlew :app:assembleTvRelease             # 打发布包（未签名）
./gradlew :core:testDebugUnitTest           # 跑全部 JVM 单元测试
./gradlew :core:testDebugUnitTest --tests "org.mz.mzdkplayer.tool.PlayerMediaTextTest"   # 跑单个测试类
./gradlew clean                            # 清理构建产物
./gradlew --stop                           # 停掉 Gradle 守护进程（锁文件冲突时用）
```

### 单元测试

- 测试只依赖 **JUnit 4**，没有引入 mockito / robolectric，也**没有开启** `returnDefaultValues`。
  因此测试必须写成纯 JVM 测试：`android.net.Uri`、`android.util.Log`、`android.util.Base64`、`Context` 等 Android 类型一旦调用就会抛 `RuntimeException("Stub!")`。
- 需要测试的纯逻辑请抽到不依赖 Android 的 `object` / `internal object` 中；测试与被测代码同在 `:core`（`internal` 对 app 不可见，这条边界是有意保留的）。
- 命名约定：类名 `XxxTest`，测试函数用反引号中文描述，例如 `` `剧集 - 标题加季集加年份` ``。
- 现有测试：`MediaInfoExtractorFormFileNameTest`（文件名解析）、`PlayerMediaTextTest`（播放页标题与日期）、`FileTimeParseTest`（HTTP 日期 / 协议推断 / 账号密码 / NFS 路径拆分）。

### 日志

项目使用 logback-android；网络协议类实现（SMB / FTP / WebDAV / NFS）都配置了超时，失败时会「软降级」返回空列表或回退本地读取，不会中断播放流程。

---

## 硬件要求

### 推荐配置

- **芯片组**：Amlogic S928X-J
- **内存**：4GB RAM及以上
- **系统**：Android TV 11及以上

### 一般配置

- **芯片组**：MT9653或同等性能芯片
- **内存**：2GB RAM
- **系统**：Android TV 7及以上

### 最低要求

- **芯片组**：晶晨S905L或同等性能芯片
- **内存**：1GB RAM
- **系统**：Android 6.0 (API 23) 及以上（`minSdk = 23`），遥控操作体验需要电视 / 盒子的方向键支持

> ⚠️ **注意**：代码写的烂，不会优化，能跑就成功，都是bug，设备性能不足可能导致视频与弹幕播放卡顿，或无法正常播放高码率视频

---

## 项目状态

⚠️ **开发阶段**：初始阶段，存在已知Bug

### 近期开发计划

- [x] FTP协议支持
- [x] WebDAV协议支持
- [x] NFS协议支持
- [x] 音频文件，图片文件支持
- [x] 播放列表管理
- [x] 电影/电视剧详情页面
- [ ] 网络弹幕加载功能
- [ ] 设置界面优化

完整的版本变更记录见 [CHANGELOG.md](CHANGELOG.md)。

---

## 贡献指南

欢迎任何形式的贡献，尤其欢迎对 **播放器稳定性** 的改进！

### 报告问题

提交 Issue 时请尽量包含：设备型号与芯片、系统版本、应用版本（`设置 -> 关于软件`）、使用的协议类型（SMB / FTP / WebDAV / NFS / HTTP 或本地）、复现步骤，以及一段 `adb logcat` 抓取的日志。带日志的 Issue 通常能很快定位。

### 提交代码流程

1. **Fork** 本仓库，从 `main` 分支切出功能分支，建议命名：

   | 前缀 | 用途 | 示例 |
   | --- | --- | --- |
   | `feat/` | 新功能 | `feat/nfs-reconnect` |
   | `fix/` | 缺陷修复 | `fix/vlc-audio-crackle` |
   | `docs/` | 文档 | `docs/readme-usage` |
   | `refactor/` | 重构 | `refactor/player-engine-interface` |

2. 提交前先在本地跑通编译与测试：

   ```bash
   ./gradlew :app:compileTvDebugKotlin
   ./gradlew :core:testDebugUnitTest
   ```

3. 提交代码，向 `main` 发起 Pull Request，并在描述中说明：改了什么问题、怎么验证的、涉及哪些模块。

### 提交信息规范

沿用仓库现有的中文描述式提交，一句话说清「改了什么」，多个改动点用逗号分隔：

```
播放页显示刮削标题与文件日期，播放进度定时落盘，重写文件名解析
```

### 代码风格与约定

- 遵循 Kotlin 官方代码风格（`kotlin.code.style=official`）。
- 业务逻辑一律放 `:core`，且不要往里引 Material：电视端唯一的设计系统是 `androidx.tv.material3`，手机端是 `androidx.compose.material3`，两者不得交叉。
- 电视端新增页面请在 `app/src/tv/java/org/mz/mzdkplayer/ui/screen/` 下建对应包，并在 `MzDKPlayerAPP.kt` 中注册路由；手机端页面放 `app/src/phone/java/org/mz/mzdkplayer/ui/phone/screen/`，路由在 `PhoneRoutes.kt`。
- 需要访问数据库的 ViewModel 请通过 `viewModelWithFactory { RepositoryProvider.xxx() }` 注入，不要在 Composable 里直接拿 DAO。
- 网络协议相关代码**必须设置超时**，失败时走「软降级」，不能阻塞播放或抛到界面线程。
- 涉及播放内核的改动，请在 `IMzPlayer` 层面实现，不要在 UI 层写 `if (exo) ... else ...`。
- 界面文案不要硬编码，加到 `res/values/strings.xml`，并同步补齐 `values-en` / `values-ja` / `values-zh-rTW`。
- 修改了用户可见行为，请同步更新 [CHANGELOG.md](CHANGELOG.md) 与 `app/build.gradle.kts` 中的 `versionName`。

### Pull Request 检查清单

- [ ] `./gradlew :app:compileTvDebugKotlin` 通过
- [ ] `./gradlew :core:testDebugUnitTest` 通过（有新增纯逻辑时补测试）
- [ ] 没有提交 `local.properties`、`app/build/`、`.gradle/`、`.idea/` 等本地文件
- [ ] 新增文案已加入各语言 `strings.xml`
- [ ] 用户可见的改动已更新 `CHANGELOG.md`（必要时更新 `versionName`）
- [ ] 在真机 / 电视上实际验证过（说明设备与系统版本）

### 关于仓库内的二进制依赖

`app/libs/` 下的 `akdanmaku.aar` 与 `lib-decoder-ffmpeg-release*.aar` 是播放器依赖的本地预编译库，已随仓库提供，**无需自行构建，也不要删除**。

### 自动构建与发布（GitHub Actions）

仓库里有两个工作流，不需要手动打包：

| 工作流 | 触发时机 | 做什么 |
| --- | --- | --- |
| `ci.yml` | 推送到 `main`、每个 PR | 编译 + 跑 JVM 单元测试，测试报告作为 artifact 上传 |
| `release.yml` | 推送到 `main`（版本号是新的）、推送 `V*` tag、手动触发 | 跑测试 → 构建签名 APK → 创建 Release 并上传三个 APK |

**发布是版本号驱动的**：`release.yml` 会读取 `app/build.gradle.kts` 里的 `versionName`，如果还没有对应的 `V<版本号>` Release，就自动构建并发布。也就是说，按约定 bump 完 `versionName` 并推送，Release 就自动出来了，不需要手动打 tag（tag 会由工作流创建，沿用历史的 `V1.17.4` 这种大写 `V` 前缀）。

Release 正文取自 [CHANGELOG.md](CHANGELOG.md) 的「未发布」段落，所以**发布前记得先把变更写进 CHANGELOG**。

发布需要以下 Secrets（`Settings → Secrets and variables → Actions`），缺任意一项工作流会跳过发布并在运行摘要里列出缺什么：

| Secret | 用途 |
| --- | --- |
| `TMDB_API_KEY` | 打包时写入 `BuildConfig.TMDB_API_KEY`，空值会让 TMDB 刮削静默失效 |
| `RELEASE_KEYSTORE_BASE64` | 签名密钥库的 base64：`base64 -w0 release.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | 密钥库口令 |
| `RELEASE_KEY_ALIAS` | 密钥别名 |
| `RELEASE_KEY_PASSWORD` | 密钥口令 |

签名材料只通过环境变量传给 Gradle（`MZDK_KEYSTORE_FILE` 等），**密钥文件不要提交到仓库**。本地没有配这些变量时，`assembleTvRelease` 会照旧产出 `app-tv-*-release-unsigned.apk`，行为与以前一致。

想重新发布同一个版本：到 Actions 页面手动运行 `Release`，并把 `force` 勾上。

---

## 免责声明

本软件仅供学习交流使用，请勿用于商业用途。使用本软件造成的任何问题，开发者不承担相关责任。

---

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 开源。

---

**注意**：杜比视界、杜比全景声、DTS-HD等功能的正常使用需要设备硬件支持，部分功能可能需要特定的音频视频设备才能获得最佳体验。
