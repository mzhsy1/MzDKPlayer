# MzDKPlayer · APKPure 上架文案（多语言）

> 版本：1.18.0 (114)　包名：`org.mz.mzdkplayer`　分类：Video Players & Editors
> 官网：https://mzdkplayer.pages.dev/　源码：https://github.com/mzhsy1/MzDKPlayer　许可：GPL-3.0

## 字段限制

| 字段 | 上限 | 说明 |
| --- | --- | --- |
| Title（应用名） | 30 字符 | 一般由 APK 内 `app_name` 自动带出，可在后台改 |
| Short description（短描述） | 80 字符 | 列表页与搜索结果里展示，是最关键的一行 |
| Full description（完整描述） | 4000 字符 | 详情页正文，支持换行与 emoji |
| What's new（更新说明） | 500 字符 | 每个版本单独填 |

短描述已按 80 字符上限校核（含中日韩字符按 1 个字符计），下方每节标注了实际字符数。

---

## 一、English (en-US)

**Title:** `MzDKPlayer: Android TV Player`

**Short description (69/80)**
```
Play your NAS library on TV: danmaku, SMB/NFS/FTP, 4K HDR, subtitles.
```

**Full description**

MzDKPlayer is a media player built for Android TV, made for people whose movies, music and photos live on a NAS, a hard drive or a home server.

VIDEO, AUDIO AND PHOTOS
Play what you already have. Browse folders directly, or use the built-in media library for movies, TV shows and music. Info comes from TMDB, and local .nfo files are supported too.

NETWORK SHARES
SMB, FTP, WebDAV, NFS and HTTP (NGINX directory listing), plus local files. Every connection uses timeouts, and a broken share never crashes the player.

SUPPORTED FORMATS
Video: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS and ISO Blu-ray images
Codecs: H.264, H.265/HEVC, AV1, VP9, MPEG-2, up to 4K/8K, HDR10 and HLG
Audio: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA, plus DTS, DTS-HD, TrueHD and AC3 tracks
Photos: JPEG, PNG, WebP, BMP and HEIC
Subtitles: external SRT, ASS, SSA, VTT, and embedded MKV, PGS, DVB, Teletext

DANMAKU
Comment overlays on top of your video. Drop a matching .xml file next to the video and it loads automatically. Font size, speed, opacity and display area are all adjustable.

TWO PLAYBACK ENGINES
Media3 ExoPlayer and libVLC, switchable at any time. ExoPlayer starts fast and is friendly to hardware decoding; VLC covers more formats and supports audio passthrough for AV receivers and soundbars.

EVERYDAY DETAILS
- Switch audio, video and subtitle tracks, adjust playback speed
- Style text subtitles: font, size, colour, background and bottom margin
- Shift subtitle timing in 0.5 s steps, up to 30 s early or late
- Remember tracks, speed and aspect ratio per file, restored on the next playback
- Playback progress saved automatically, resume from history
- Configurable remote keys, designed for D-pad remotes

YOUR PHONE AS A REMOTE
Turn on the LAN remote in the app, scan the QR code with your phone, and the browser becomes a remote control. No extra app needed.

INTERFACE LANGUAGES
Simplified Chinese, English, Japanese and Traditional Chinese.

REQUIREMENTS
Android 6.0 or newer, on a TV or box with a D-pad remote. Android TV 7 or newer with 2 GB of RAM is recommended. Dolby Vision, DTS-HD and similar formats require compatible hardware.
On Android 11 and newer, choose "Allow management of all files" so the player can scan your folders.

MzDKPlayer is free, with no ads, no account and no in-app purchases. Open source under GPL-3.0.

---

## 二、简体中文 (zh-CN)

**Title:** `MzDKPlayer：安卓电视播放器`

**Short description (32/80)**
```
专为安卓电视设计的本地影音播放器，支持弹幕与 NAS 网络协议。
```

**Full description**

MzDKPlayer 是一款为安卓电视打造的本地媒体播放器，适合把电影、音乐与照片放在 NAS、硬盘或家用服务器上的用户。

视频 · 音频 · 图片
播放你手上已有的文件。可以按目录浏览，也能用内置媒体库管理电影、电视剧与音乐，影视信息来自 TMDB，同时支持读取本地 .nfo 文件。

网络存储
支持 SMB、FTP、WebDAV、NFS 与 HTTP（NGINX 目录列表）以及本地文件。所有连接都设置了超时并做了软降级处理，某个共享目录异常不会让播放器崩溃。

格式支持
视频：MP4、MKV、MOV、AVI、WMV、FLV、WebM、TS、M2TS、MTS，以及 ISO 蓝光原盘镜像
编码：H.264、H.265/HEVC、AV1、VP9、MPEG-2，支持 4K/8K、HDR10 与 HLG
音频：FLAC、WAV、ALAC、MP3、AAC、OGG、Opus、WMA，以及 DTS、DTS-HD、TrueHD、AC3 音轨
图片：JPEG、PNG、WebP、BMP、HEIC
字幕：外挂 SRT、ASS、SSA、VTT，内嵌 MKV、PGS、DVB、Teletext

弹幕
弹幕评论叠加显示。把同名的 .xml 弹幕文件放在视频旁边即可自动加载，字号、速度、透明度、显示区域均可调节。

双播放内核
内置 Media3 ExoPlayer 与 libVLC 两套内核，随时切换。ExoPlayer 启动快、对硬件解码友好；VLC 兼容性更广，并支持音频直通，方便连接功放与回音壁。

日常细节
· 音轨、视频轨、字幕轨自由切换，支持倍速播放
· 文本字幕可自定义字体、字号、颜色、背景色与底部边距
· 字幕时间轴微调，0.5 秒步进，最多提前或延后 30 秒
· 按文件记住音轨、字幕、倍速与画面比例，再次播放自动恢复
· 播放进度自动保存，可从历史记录继续播放
· 遥控器上下键功能可自定义，适配方向键操作

手机当遥控器
在应用内开启局域网遥控，手机扫码后浏览器页面即可当遥控器，不用额外安装 App。

界面语言
简体中文 / English / 日本語 / 繁體中文。

运行要求
Android 6.0 及以上，需要带方向键的电视或电视盒子；推荐 Android TV 7 及以上、2GB 内存。杜比视界、DTS-HD 等格式需要设备硬件支持。
Android 11 及以上首次启动请选择「允许管理所有文件」，否则部分目录扫不到。

MzDKPlayer 完全免费，无广告、无需注册、无内购，基于 GPL-3.0 开源。

---

## 三、繁體中文 (zh-TW)

**Title:** `MzDKPlayer：安卓電視播放器`

**Short description (32/80)**
```
專為安卓電視設計的本地影音播放器，支援彈幕與 NAS 網路協定。
```

**Full description**

MzDKPlayer 是一款為安卓電視打造的本地媒體播放器，適合把電影、音樂與照片放在 NAS、硬碟或家用伺服器的使用者。

影片 · 音樂 · 圖片
播放你手上的檔案。可以依目錄瀏覽，也能用內建媒體庫管理電影、影集與音樂，影視資訊來自 TMDB，同時支援讀取本地 .nfo。

網路儲存
支援 SMB、FTP、WebDAV、NFS 與 HTTP（NGINX 目錄列表）以及本地檔案。所有連線都設有逾時並做了軟降級處理，某個共享目錄異常不會讓播放器崩潰。

格式支援
影片：MP4、MKV、MOV、AVI、WMV、FLV、WebM、TS、M2TS、MTS，以及 ISO 藍光原盤映像
編碼：H.264、H.265/HEVC、AV1、VP9、MPEG-2，支援 4K/8K、HDR10 與 HLG
音訊：FLAC、WAV、ALAC、MP3、AAC、OGG、Opus、WMA，以及 DTS、DTS-HD、TrueHD、AC3 音軌
圖片：JPEG、PNG、WebP、BMP、HEIC
字幕：外掛 SRT、ASS、SSA、VTT，內嵌 MKV、PGS、DVB、Teletext

彈幕
彈幕評論疊加顯示。把同名的 .xml 彈幕檔放在影片旁邊就會自動載入，字級、速度、透明度、顯示區域都能調整。

雙播放核心
內建 Media3 ExoPlayer 與 libVLC 兩套核心，隨時切換。ExoPlayer 啟動快、對硬體解碼友善；VLC 相容性更廣，並支援音訊直通，方便連接擴大機與聲霸。

日常細節
· 音軌、視訊軌、字幕軌自由切換，支援倍速播放
· 文字字幕可自訂字型、字級、顏色、背景色與底部邊距
· 字幕時間軸微調，0.5 秒步進，最多提前或延後 30 秒
· 依檔案記住音軌、字幕、倍速與畫面比例，再次播放自動還原
· 播放進度自動儲存，可從歷史記錄續播
· 遙控器上下鍵功能可自訂，適合格線鍵操作

手機當遙控器
在應用程式內開啟區域網路遙控，手機掃碼後瀏覽器頁面就能當遙控器，不必額外安裝 App。

介面語言
简体中文 / English / 日本語 / 繁體中文。

執行需求
Android 6.0 以上，需要具備方向鍵的電視或電視盒；建議 Android TV 7 以上、2GB 記憶體。杜比視界、DTS-HD 等格式需要裝置硬體支援。
Android 11 以上首次啟動請選擇「允許管理所有檔案」，否則部分目錄掃描不到。

MzDKPlayer 完全免費，無廣告、免註冊、無內購，採用 GPL-3.0 開源授權。

---

## 四、日本語 (ja-JP)

**Title:** `MzDKPlayer：Android TV プレーヤー`

**Short description (48/80)**
```
Android TV向けメディアプレーヤー。コメント弾幕・SMB/NFS対応、4K HDR再生。
```

**Full description**

MzDKPlayer は Android TV 向けに作られたメディアプレーヤーです。NAS や外付けハードディスク、自宅サーバーに保存した動画・音楽・写真を、テレビの大画面でそのまま再生できます。

■ 動画・音楽・写真
手持ちのファイルをそのまま再生。フォルダをたどる操作のほか、内蔵メディアライブラリで映画・ドラマ・音楽を管理できます。作品情報は TMDB から取得し、ローカルの .nfo ファイルの読み込みにも対応します。

■ ネットワーク共有
SMB / FTP / WebDAV / NFS / HTTP（NGINX のディレクトリ一覧）とローカルファイルに対応。すべての接続にタイムアウトを設定し、接続に失敗しても再生画面が落ちないよう安全に処理します。

■ 対応フォーマット
動画：MP4、MKV、MOV、AVI、WMV、FLV、WebM、TS、M2TS、MTS、ISO（Blu-ray イメージ）
コーデック：H.264、H.265/HEVC、AV1、VP9、MPEG-2、4K/8K・HDR10・HLG
音声：FLAC、WAV、ALAC、MP3、AAC、OGG、Opus、WMA、DTS、DTS-HD、TrueHD、AC3
画像：JPEG、PNG、WebP、BMP、HEIC
字幕：外部字幕 SRT・ASS・SSA・VTT、内蔵字幕 MKV・PGS・DVB・Teletext

■ コメント弾幕
動画と同じフォルダに同名の .xml ファイルを置くだけで自動的に読み込みます。文字サイズ、速度、透過度、表示範囲を調整できます。

■ 2 つの再生エンジン
Media3 ExoPlayer と libVLC を搭載し、いつでも切り替えられます。ExoPlayer は起動が速くハードウェアデコードに強く、VLC は対応フォーマットが広く、AV アンプ向けの音声パススルーにも対応します。

■ 使い勝手
・音声・映像・字幕トラックの切り替え、再生速度の変更
・テキスト字幕のフォント、サイズ、色、背景、下余白の調整
・字幕タイミングを 0.5 秒刻みで最大 ±30 秒ずらす補正
・ファイルごとにトラック・速度・画面比率を記憶し、次回の再生で自動復元
・再生位置の自動保存と、履歴からの再開
・リモコンの上下キーなどボタンの動作をカスタマイズ

■ スマホをリモコンに
アプリ内で LAN リモートを有効にすると QR コードが表示されます。スマホで読み取ればブラウザがそのままリモコンになります。追加アプリは不要です。

■ 表示言語
简体中文 / English / 日本語 / 繁體中文

■ 動作環境
Android 6.0 以上、方向キー付きのテレビまたはボックスが必要です。Android TV 7 以上・メモリ 2GB 以上を推奨。Dolby Vision や DTS-HD などの再生には対応ハードウェアが必要です。
Android 11 以上では初回起動時に「すべてのファイルへのアクセスを許可」を選ぶと、フォルダを正しくスキャンできます。

MzDKPlayer は完全無料。広告なし・登録不要・アプリ内課金なしのオープンソースソフトウェア（GPL-3.0）です。

---

## 五、한국어 (ko-KR)

**Title:** `MzDKPlayer : Android TV 플레이어`

**Short description (52/80)**
```
Android TV용 미디어 플레이어. 탄막, SMB/NFS/FTP, 4K HDR 재생 지원.
```

**Full description**

MzDKPlayer는 Android TV를 위해 만든 미디어 플레이어입니다. NAS나 외장 하드, 홈 서버에 보관한 영상, 음악, 사진을 TV 화면에서 그대로 재생할 수 있습니다.

■ 영상 · 음악 · 사진
폴더를 직접 탐색하거나 내장 미디어 라이브러리로 영화, 드라마, 음악을 관리할 수 있습니다. 작품 정보는 TMDB에서 가져오며, 로컬 .nfo 파일 읽기도 지원합니다.

■ 네트워크 공유
SMB / FTP / WebDAV / NFS / HTTP(NGINX 디렉터리 목록)와 로컬 파일을 지원합니다. 모든 연결에 타임아웃을 두고, 연결이 실패해도 재생 화면이 종료되지 않도록 처리했습니다.

■ 지원 형식
영상: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS, ISO(블루레이 이미지)
코덱: H.264, H.265/HEVC, AV1, VP9, MPEG-2, 4K/8K, HDR10, HLG
음악: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA, DTS, DTS-HD, TrueHD, AC3
이미지: JPEG, PNG, WebP, BMP, HEIC
자막: 외부 자막 SRT·ASS·SSA·VTT, 내장 자막 MKV·PGS·DVB·Teletext

■ 탄막(댓글 오버레이)
영상과 같은 폴더에 같은 이름의 .xml 파일을 두면 자동으로 불러옵니다. 글자 크기, 속도, 투명도, 표시 영역을 조절할 수 있습니다.

■ 두 가지 재생 엔진
Media3 ExoPlayer와 libVLC를 내장했고 언제든 전환할 수 있습니다. ExoPlayer는 실행이 빠르고 하드웨어 디코딩에 강하며, VLC는 지원 형식이 넓고 AV 리시버용 오디오 패스스루를 지원합니다.

■ 편의 기능
· 오디오·비디오·자막 트랙 전환, 재생 속도 조절
· 텍스트 자막의 글꼴, 크기, 색상, 배경, 하단 여백 설정
· 자막 타이밍을 0.5초 단위로 최대 ±30초 보정
· 파일별로 트랙·속도·화면 비율을 기억해 다음 재생 시 자동 복원
· 재생 위치 자동 저장, 기록에서 이어보기
· 리모컨 상하 키 등 버튼 동작 사용자 지정

■ 스마트폰을 리모컨으로
앱에서 LAN 리모컨을 켜면 QR 코드가 표시됩니다. 스마트폰으로 스캔하면 브라우저가 그대로 리모컨이 됩니다. 별도 앱이 필요 없습니다.

■ 표시 언어
简体中文 / English / 日本語 / 繁體中文

■ 실행 환경
Android 6.0 이상, 방향키가 있는 TV 또는 셋톱박스가 필요합니다. Android TV 7 이상, RAM 2GB 이상을 권장합니다. Dolby Vision, DTS-HD 등은 하드웨어 지원이 필요합니다.
Android 11 이상에서는 첫 실행 시 "모든 파일 관리 허용"을 선택해야 폴더를 제대로 검색할 수 있습니다.

MzDKPlayer는 완전 무료이며 광고, 회원가입, 인앱 결제가 없습니다. GPL-3.0 오픈소스입니다.

---

## 六、Русский (ru-RU)

**Title:** `MzDKPlayer: плеер для Android TV`

**Short description (66/80)**
```
Медиаплеер для Android TV: данмаку, SMB/NFS/FTP, 4K HDR, субтитры.
```

**Full description**

MzDKPlayer — медиаплеер для Android TV. Он создан для тех, у кого фильмы, музыка и фото хранятся на NAS, внешнем диске или домашнем сервере.

■ Видео, музыка и фото
Воспроизводите то, что у вас уже есть: просматривайте папки напрямую или пользуйтесь встроенной медиатекой для фильмов, сериалов и музыки. Сведения о фильмах загружаются из TMDB, поддерживаются и локальные файлы .nfo.

■ Сетевые ресурсы
SMB, FTP, WebDAV, NFS, HTTP (список каталогов NGINX) и локальные файлы. У всех подключений заданы тайм-ауты, и сбой сети не приводит к падению плеера.

■ Поддерживаемые форматы
Видео: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS и ISO-образы Blu-ray
Кодеки: H.264, H.265/HEVC, AV1, VP9, MPEG-2, вплоть до 4K/8K, HDR10 и HLG
Аудио: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA, а также дорожки DTS, DTS-HD, TrueHD и AC3
Изображения: JPEG, PNG, WebP, BMP, HEIC
Субтитры: внешние SRT, ASS, SSA, VTT и встроенные MKV, PGS, DVB, Teletext

■ Данмаку
Комментарии, пролетающие поверх видео. Положите рядом с видео файл .xml с тем же именем — он загрузится автоматически. Размер шрифта, скорость, прозрачность и область показа настраиваются.

■ Два движка воспроизведения
Media3 ExoPlayer и libVLC с возможностью переключения в любой момент. ExoPlayer быстро запускается и хорошо работает с аппаратным декодированием, VLC шире по форматам и поддерживает аудио-пасссру (passthrough) для ресиверов и саундбаров.

■ Полезные мелочи
• Выбор аудио-, видео- и субтитровых дорожек, скорость воспроизведения
• Настройка шрифта, размера, цвета, фона и нижнего отступа субтитров
• Сдвиг субтитров шагом 0,5 с, до ±30 с
• Запоминание дорожек, скорости и пропорций для каждого файла
• Автосохранение позиции и продолжение просмотра из истории
• Переназначение кнопок пульта

■ Смартфон как пульт
Включите пульт в локальной сети — появится QR-код. Отсканируйте его телефоном, и браузер станет пультом. Дополнительное приложение не нужно.

■ Языки интерфейса
简体中文 / English / 日本語 / 繁體中文

■ Требования
Android 6.0 и новее, телевизор или ТВ-приставка с пультом. Рекомендуется Android TV 7 и выше, 2 ГБ ОЗУ. Dolby Vision, DTS-HD и подобные форматы требуют совместимого оборудования.
На Android 11 и новее при первом запуске выберите «Разрешить управление всеми файлами», иначе часть папок не будет найдена.

MzDKPlayer бесплатный: без рекламы, регистрации и встроенных покупок. Открытый код под лицензией GPL-3.0.

---

## 七、Español (es-ES)

**Title:** `MzDKPlayer: reproductor Android TV`

**Short description (71/80)**
```
Reproductor para Android TV: danmaku, SMB/NFS/FTP, 4K HDR y subtítulos.
```

**Full description**

MzDKPlayer es un reproductor multimedia diseñado para Android TV, pensado para quienes guardan sus películas, música y fotos en un NAS, un disco externo o un servidor doméstico.

■ Vídeo, música y fotos
Reproduce lo que ya tienes: navega por carpetas o usa la biblioteca integrada para películas, series y música. La información se obtiene de TMDB y también admite archivos .nfo locales.

■ Recursos de red
SMB, FTP, WebDAV, NFS y HTTP (listado de directorios de NGINX), además de archivos locales. Todas las conexiones tienen tiempo de espera y un fallo de red no cierra el reproductor.

■ Formatos compatibles
Vídeo: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS e imágenes ISO de Blu-ray
Códecs: H.264, H.265/HEVC, AV1, VP9, MPEG-2, hasta 4K/8K, HDR10 y HLG
Audio: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA, y pistas DTS, DTS-HD, TrueHD y AC3
Imágenes: JPEG, PNG, WebP, BMP y HEIC
Subtítulos: externos SRT, ASS, SSA y VTT, e integrados MKV, PGS, DVB y Teletext

■ Danmaku
Comentarios superpuestos al vídeo. Coloca junto al vídeo un archivo .xml con el mismo nombre y se cargará solo. Puedes ajustar tamaño de letra, velocidad, opacidad y zona de visualización.

■ Dos motores de reproducción
Media3 ExoPlayer y libVLC, intercambiables en cualquier momento. ExoPlayer arranca rápido y aprovecha la decodificación por hardware; VLC admite más formatos y el paso de audio (passthrough) para amplificadores y barras de sonido.

■ Detalles del día a día
• Selección de pistas de audio, vídeo y subtítulos, y velocidad de reproducción
• Estilo de subtítulos de texto: fuente, tamaño, color, fondo y margen inferior
• Desfase de subtítulos en pasos de 0,5 s, hasta ±30 s
• Recuerda pistas, velocidad y relación de aspecto por archivo
• Progreso guardado automáticamente y reanudación desde el historial
• Teclas del mando configurables

■ El móvil como mando
Activa el mando por red local y aparecerá un código QR. Escanéalo con el teléfono y el navegador se convierte en mando. No hace falta otra aplicación.

■ Idiomas
简体中文 / English / 日本語 / 繁體中文

■ Requisitos
Android 6.0 o superior, en un televisor o decodificador con mando de cruceta. Se recomienda Android TV 7 o superior y 2 GB de RAM. Dolby Vision, DTS-HD y formatos similares necesitan hardware compatible.
En Android 11 o superior, elige «Permitir la administración de todos los archivos» para que el reproductor pueda escanear tus carpetas.

MzDKPlayer es gratis, sin anuncios, sin cuenta y sin compras integradas. Código abierto bajo GPL-3.0.

---

## 八、Português (pt-BR)

**Title:** `MzDKPlayer: player para Android TV`

**Short description (64/80)**
```
Player para Android TV: danmaku, SMB/NFS/FTP, 4K HDR e legendas.
```

**Full description**

MzDKPlayer é um reprodutor de mídia feito para Android TV, pensado para quem guarda filmes, músicas e fotos em um NAS, HD externo ou servidor doméstico.

■ Vídeo, música e fotos
Reproduza o que você já tem: navegue pelas pastas ou use a biblioteca integrada para filmes, séries e músicas. As informações vêm do TMDB, e arquivos .nfo locais também são aceitos.

■ Compartilhamentos de rede
SMB, FTP, WebDAV, NFS e HTTP (listagem de diretórios do NGINX), além de arquivos locais. Todas as conexões têm timeout, e uma falha de rede não fecha o player.

■ Formatos compatíveis
Vídeo: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS e imagens ISO de Blu-ray
Codecs: H.264, H.265/HEVC, AV1, VP9, MPEG-2, até 4K/8K, HDR10 e HLG
Áudio: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA e faixas DTS, DTS-HD, TrueHD e AC3
Imagens: JPEG, PNG, WebP, BMP e HEIC
Legendas: externas SRT, ASS, SSA e VTT, e embutidas MKV, PGS, DVB e Teletext

■ Danmaku
Comentários sobrepostos ao vídeo. Basta deixar um arquivo .xml com o mesmo nome ao lado do vídeo para carregar automaticamente. Tamanho da fonte, velocidade, opacidade e área de exibição são ajustáveis.

■ Dois motores de reprodução
Media3 ExoPlayer e libVLC, com troca a qualquer momento. O ExoPlayer inicia rápido e usa decodificação por hardware; o VLC cobre mais formatos e oferece passthrough de áudio para receivers e soundbars.

■ No dia a dia
• Escolha de faixas de áudio, vídeo e legenda, e velocidade de reprodução
• Estilo de legendas de texto: fonte, tamanho, cor, fundo e margem inferior
• Ajuste de sincronia da legenda em passos de 0,5 s, até ±30 s
• Lembra faixas, velocidade e proporção por arquivo
• Progresso salvo automaticamente e retomada pelo histórico
• Teclas do controle remoto configuráveis

■ Celular como controle remoto
Ative o controle pela rede local e um QR Code aparece. Escaneie com o celular e o navegador vira o controle. Não precisa de outro app.

■ Idiomas
简体中文 / English / 日本語 / 繁體中文

■ Requisitos
Android 6.0 ou superior, em uma TV ou box com controle direcional. Recomendamos Android TV 7 ou superior e 2 GB de RAM. Dolby Vision, DTS-HD e formatos semelhantes exigem hardware compatível.
No Android 11 ou superior, escolha "Permitir gerenciamento de todos os arquivos" para que as pastas sejam escaneadas.

MzDKPlayer é gratuito, sem anúncios, sem cadastro e sem compras no app. Código aberto sob GPL-3.0.

---

## 九、Deutsch (de-DE)

**Title:** `MzDKPlayer: Player für Android TV`

**Short description (70/80)**
```
Medienplayer für Android TV: Danmaku, SMB/NFS/FTP, 4K HDR, Untertitel.
```

**Full description**

MzDKPlayer ist ein Mediaplayer für Android TV – für alle, die Filme, Musik und Fotos auf einem NAS, einer externen Festplatte oder einem Heimserver liegen haben.

■ Video, Musik und Fotos
Geben Sie wieder, was Sie schon haben: Ordner direkt durchsuchen oder die integrierte Mediathek für Filme, Serien und Musik nutzen. Infos kommen von TMDB, lokale .nfo-Dateien werden ebenfalls gelesen.

■ Netzwerkfreigaben
SMB, FTP, WebDAV, NFS und HTTP (NGINX-Verzeichnisliste) sowie lokale Dateien. Alle Verbindungen haben Timeouts, und ein Netzwerkfehler lässt den Player nicht abstürzen.

■ Unterstützte Formate
Video: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS und ISO-Blu-ray-Images
Codecs: H.264, H.265/HEVC, AV1, VP9, MPEG-2, bis 4K/8K, HDR10 und HLG
Audio: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA sowie DTS-, DTS-HD-, TrueHD- und AC3-Spuren
Bilder: JPEG, PNG, WebP, BMP und HEIC
Untertitel: extern SRT, ASS, SSA, VTT und eingebettet MKV, PGS, DVB, Teletext

■ Danmaku
Kommentare, die über das Video laufen. Eine gleichnamige .xml-Datei neben dem Video genügt, sie wird automatisch geladen. Schriftgröße, Tempo, Deckkraft und Anzeigebereich sind einstellbar.

■ Zwei Wiedergabe-Engines
Media3 ExoPlayer und libVLC, jederzeit umschaltbar. ExoPlayer startet schnell und nutzt Hardware-Dekodierung, VLC deckt mehr Formate ab und unterstützt Audio-Passthrough für Receiver und Soundbars.

■ Alltagsdetails
• Audio-, Video- und Untertitelspuren wählen, Wiedergabegeschwindigkeit ändern
• Textuntertitel gestalten: Schrift, Größe, Farbe, Hintergrund und unterer Abstand
• Untertitel um 0,5 s verschieben, bis ±30 s
• Spuren, Tempo und Seitenverhältnis pro Datei merken
• Fortschritt wird automatisch gespeichert, Fortsetzen aus dem Verlauf
• Tasten der Fernbedienung frei belegbar

■ Das Handy als Fernbedienung
Fernbedienung im lokalen Netz aktivieren, QR-Code anzeigen lassen und mit dem Handy scannen – der Browser wird zur Fernbedienung. Keine zusätzliche App nötig.

■ Sprachen
简体中文 / English / 日本語 / 繁體中文

■ Voraussetzungen
Android 6.0 oder neuer, auf einem Fernseher oder einer Box mit Steuerkreuz-Fernbedienung. Empfohlen: Android TV 7 oder neuer und 2 GB RAM. Dolby Vision, DTS-HD und Ähnliches benötigen passende Hardware.
Unter Android 11 oder neuer bitte „Zugriff auf alle Dateien verwalten" erlauben, sonst werden manche Ordner nicht gefunden.

MzDKPlayer ist kostenlos, werbefrei, ohne Konto und ohne In-App-Käufe. Open Source unter GPL-3.0.

---

## 十、Français (fr-FR)

**Title:** `MzDKPlayer : lecteur Android TV`

**Short description (70/80)**
```
Lecteur pour Android TV : danmaku, SMB/NFS/FTP, 4K HDR et sous-titres.
```

**Full description**

MzDKPlayer est un lecteur multimédia conçu pour Android TV, pensé pour celles et ceux qui stockent films, musique et photos sur un NAS, un disque externe ou un serveur maison.

■ Vidéo, musique et photos
Lisez ce que vous possédez déjà : parcourez vos dossiers ou utilisez la médiathèque intégrée pour les films, les séries et la musique. Les informations proviennent de TMDB, avec prise en charge des fichiers .nfo locaux.

■ Partages réseau
SMB, FTP, WebDAV, NFS et HTTP (liste de répertoires NGINX), ainsi que les fichiers locaux. Chaque connexion a un délai d'attente et une panne réseau ne fait pas planter le lecteur.

■ Formats pris en charge
Vidéo : MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS et images ISO Blu-ray
Codecs : H.264, H.265/HEVC, AV1, VP9, MPEG-2, jusqu'à 4K/8K, HDR10 et HLG
Audio : FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA, ainsi que les pistes DTS, DTS-HD, TrueHD et AC3
Images : JPEG, PNG, WebP, BMP et HEIC
Sous-titres : externes SRT, ASS, SSA, VTT et intégrés MKV, PGS, DVB, Teletext

■ Danmaku
Des commentaires défilent par-dessus la vidéo. Placez un fichier .xml du même nom à côté de la vidéo : il est chargé automatiquement. Taille, vitesse, opacité et zone d'affichage sont réglables.

■ Deux moteurs de lecture
Media3 ExoPlayer et libVLC, permutables à tout moment. ExoPlayer démarre vite et profite du décodage matériel ; VLC couvre plus de formats et gère le passthrough audio pour les amplis et barres de son.

■ Les détails du quotidien
• Choix des pistes audio, vidéo et sous-titres, vitesse de lecture
• Style des sous-titres texte : police, taille, couleur, fond et marge basse
• Décalage des sous-titres par pas de 0,5 s, jusqu'à ±30 s
• Mémorisation des pistes, de la vitesse et du format d'image par fichier
• Progression enregistrée automatiquement et reprise depuis l'historique
• Touches de la télécommande personnalisables

■ Le téléphone comme télécommande
Activez la télécommande en réseau local : un QR code s'affiche. Scannez-le avec votre téléphone et le navigateur devient la télécommande. Aucune application supplémentaire.

■ Langues
简体中文 / English / 日本語 / 繁體中文

■ Configuration requise
Android 6.0 ou plus, sur un téléviseur ou un boîtier avec télécommande à flèches. Android TV 7 ou plus et 2 Go de RAM recommandés. Dolby Vision, DTS-HD et formats similaires nécessitent du matériel compatible.
Sur Android 11 ou plus, choisissez « Autoriser la gestion de tous les fichiers » pour que les dossiers soient analysés.

MzDKPlayer est gratuit, sans publicité, sans compte et sans achats intégrés. Open source sous GPL-3.0.

---

## 十一、العربية (ar)

**Title:** `MzDKPlayer: مشغل أندرويد TV`

**Short description (65/80)**
```
مشغل وسائط لأندرويد TV: دانماكو، SMB/NFS/FTP، عرض 4K HDR وترجمات.
```

**Full description**

MzDKPlayer مشغل وسائط مصمم لأجهزة Android TV، لمن يحفظون أفلامهم وموسيقاهم وصورهم على وحدة تخزين شبكية أو قرص خارجي أو خادم منزلي.

■ الفيديو والموسيقى والصور
شغّل ما لديك بالفعل: تصفّح المجلدات مباشرة أو استخدم مكتبة الوسائط المدمجة للأفلام والمسلسلات والموسيقى. تُجلب معلومات الأعمال من TMDB، مع دعم ملفات .nfo المحلية.

■ مشاركات الشبكة
SMB وFTP وWebDAV وNFS وHTTP (قائمة مجلدات NGINX) إضافة إلى الملفات المحلية. جميع الاتصالات لها مهلة زمنية، وانقطاع الشبكة لا يؤدي إلى إغلاق المشغل.

■ الصيغ المدعومة
الفيديو: MP4 وMKV وMOV وAVI وWMV وFLV وWebM وTS وM2TS وMTS وصور ISO لأقراص Blu-ray
الترميزات: H.264 وH.265/HEVC وAV1 وVP9 وMPEG-2، حتى 4K/8K مع HDR10 وHLG
الصوت: FLAC وWAV وALAC وMP3 وAAC وOGG وOpus وWMA، ومسارات DTS وDTS-HD وTrueHD وAC3
الصور: JPEG وPNG وWebP وBMP وHEIC
الترجمات: ملفات خارجية SRT وASS وSSA وVTT، ومدمجة MKV وPGS وDVB وTeletext

■ الدانماكو
تعليقات تظهر فوق الفيديو. ضع ملف .xml بالاسم نفسه بجوار الفيديو ليُحمَّل تلقائيًا، ويمكن ضبط حجم الخط والسرعة والشفافية ومنطقة العرض.

■ محرّكا تشغيل
Media3 ExoPlayer وlibVLC مع إمكانية التبديل في أي وقت. يبدأ ExoPlayer بسرعة ويستفيد من فك الترميز العتادي، بينما يدعم VLC صيغًا أكثر ويمرّر الصوت (passthrough) إلى أجهزة الاستقبال ومكبرات الصوت.

■ تفاصيل يومية
• اختيار مسارات الصوت والفيديو والترجمة وسرعة التشغيل
• تنسيق الترجمات النصية: الخط والحجم واللون والخلفية والهامش السفلي
• إزاحة الترجمة بخطوات 0.5 ثانية حتى ±30 ثانية
• تذكّر المسارات والسرعة ونسبة العرض لكل ملف
• حفظ موضع التشغيل تلقائيًا والاستكمال من السجل
• إمكانية تخصيص أزرار جهاز التحكم

■ هاتفك كجهاز تحكم
فعّل التحكم عبر الشبكة المحلية ليظهر رمز QR، وامسحه بهاتفك ليصبح المتصفح جهاز تحكم. لا حاجة لتطبيق إضافي.

■ لغات الواجهة
简体中文 / English / 日本語 / 繁體中文

■ المتطلبات
أندرويد 6.0 أو أحدث، وتلفاز أو جهاز استقبال بجهاز تحكم بأزرار الاتجاهات. يُنصح بأندرويد TV 7 أو أحدث وذاكرة 2 غيغابايت. تتطلب Dolby Vision وDTS-HD وما شابهها عتادًا متوافقًا.
في أندرويد 11 أو أحدث، اختر «السماح بإدارة جميع الملفات» ليتمكن المشغل من فحص مجلداتك.

MzDKPlayer مجاني تمامًا، بلا إعلانات ولا تسجيل ولا مشتريات داخل التطبيق، ومفتوح المصدر بموجب GPL-3.0.

---

## 十二、Bahasa Indonesia (id-ID)

**Title:** `MzDKPlayer: Pemutar Android TV`

**Short description (63/80)**
```
Pemutar Android TV: danmaku, SMB/NFS/FTP, 4K HDR, dan subtitle.
```

**Full description**

MzDKPlayer adalah pemutar media yang dibuat untuk Android TV, cocok bagi Anda yang menyimpan film, musik, dan foto di NAS, hard disk eksternal, atau server rumahan.

■ Video, musik, dan foto
Putar file yang sudah Anda miliki: telusuri folder langsung atau gunakan pustaka media bawaan untuk film, serial, dan musik. Informasi diambil dari TMDB, dan file .nfo lokal juga didukung.

■ Berbagi jaringan
SMB, FTP, WebDAV, NFS, dan HTTP (daftar direktori NGINX), plus file lokal. Semua koneksi memakai timeout, dan kegagalan jaringan tidak membuat pemutar tertutup.

■ Format yang didukung
Video: MP4, MKV, MOV, AVI, WMV, FLV, WebM, TS, M2TS, MTS, dan ISO image Blu-ray
Codec: H.264, H.265/HEVC, AV1, VP9, MPEG-2, hingga 4K/8K, HDR10, dan HLG
Audio: FLAC, WAV, ALAC, MP3, AAC, OGG, Opus, WMA, serta trek DTS, DTS-HD, TrueHD, dan AC3
Gambar: JPEG, PNG, WebP, BMP, dan HEIC
Subtitle: eksternal SRT, ASS, SSA, VTT dan tertanam MKV, PGS, DVB, Teletext

■ Danmaku
Komentar yang melintas di atas video. Letakkan file .xml dengan nama sama di samping video, otomatis dimuat. Ukuran huruf, kecepatan, transparansi, dan area tampil bisa diatur.

■ Dua mesin pemutaran
Media3 ExoPlayer dan libVLC, bisa diganti kapan saja. ExoPlayer cepat dibuka dan ramah dekode perangkat keras, VLC lebih luas dalam format dan mendukung passthrough audio untuk receiver dan soundbar.

■ Detail harian
• Pilih trek audio, video, dan subtitle, serta atur kecepatan putar
• Gaya subtitle teks: font, ukuran, warna, latar, dan jarak bawah
• Geser waktu subtitle per 0,5 detik hingga ±30 detik
• Mengingat trek, kecepatan, dan rasio layar per file
• Progres tersimpan otomatis dan bisa dilanjutkan dari riwayat
• Tombol remote bisa dikustomisasi

■ Ponsel sebagai remote
Aktifkan remote jaringan lokal, lalu pindai kode QR dengan ponsel dan browser menjadi remote. Tidak perlu aplikasi tambahan.

■ Bahasa
简体中文 / English / 日本語 / 繁體中文

■ Persyaratan
Android 6.0 atau lebih baru, pada TV atau kotak dengan remote tombol arah. Disarankan Android TV 7 atau lebih baru dan RAM 2 GB. Dolby Vision, DTS-HD, dan sejenisnya butuh perangkat keras yang mendukung.
Di Android 11 atau lebih baru, pilih "Izinkan pengelolaan semua file" agar folder dapat dipindai.

MzDKPlayer gratis, tanpa iklan, tanpa akun, dan tanpa pembelian dalam aplikasi. Sumber terbuka dengan lisensi GPL-3.0.

---

## 十三、版本更新说明（What's new · 1.18.0）

**English (479/500)**

```
New in 1.18.0

Added
- Subtitle timing offset: shift subtitles ±30 s in 0.5 s steps
- Per-file preferences: audio track, subtitle track, speed and aspect ratio
- Scraped title and file date on the player; progress saved automatically

Improved
- Settings reorganised into 9 categories; filename parser rewritten

Fixed
- Screen saver no longer appears when the next episode starts
- NFS paths no longer contain double slashes
- Bad links no longer break an HTTP directory listing
```

**简体中文 (263/500)**

```
1.18.0 更新内容

新增
- 字幕时间轴微调：以 0.5 秒为步进，最多 ±30 秒，外挂与内嵌文本字幕均生效
- 按文件记住播放偏好：音轨、字幕轨、倍速与画面比例，再次播放自动恢复
- 播放页显示刮削标题与文件日期，播放进度自动保存
- 设置页新增全局画面比例、播放完成动作与批量扫描层级入口

优化
- 设置页重新整理为 9 类，新增「遥控器与交互」「刮削与媒体库」
- 文件名解析重写：支持更多季集写法，片名更干净

修复
- 修复切集后跳屏保、NFS 路径出现双斜杠、HTTP 目录页因无效链接整页加载失败等问题
```

---

## 十四、提交前的几件事

**1. 应用名**
APKPure 的 Title 由 APK 内的 `app_name` 决定（当前为 `MzDKPlayer`）。如果后台允许单独填写，可按上面各语言的写法补上「Android TV Player」这类关键词，对搜索更有利；不允许就保持包内名称即可。

**2. 分类与标签**
- 分类：Video Players & Editors（备选 Media & Video）
- 建议标签：Video Player / Media Player / NAS / Android TV / Subtitle / Danmaku / 4K

**3. 权限说明（APKPure 一般会问，最好提前准备）**
| 权限 | 用途说明 |
| --- | --- |
| 存储 / 所有文件访问 | 扫描并播放本地视频、音乐、图片，读写同目录下的字幕与弹幕文件 |
| 网络 | 访问 SMB / FTP / WebDAV / NFS / HTTP 网络存储，以及 TMDB 刮削 |
| 麦克风（RECORD_AUDIO） | 仅用于音频播放页的频谱可视化效果，不录音、不上传。**若该功能已不再使用，建议直接从清单里删掉这条权限**，能少一轮审核询问 |
| 通知 | 播放状态提示 |

**4. 隐私政策**
APKPure 的详情页通常要求提供隐私政策链接。目前官网 https://mzdkplayer.pages.dev/ 如果没有这一页，建议补一个静态页并填上去，成本很低。

**5. 图文素材**
- 图标：512×512 PNG（现有 `ic_launcher` 可导出）
- 电视横幅：TV 应用还需要 320×180 的 banner（包内已有 `ic_banner`）
- 截图：`screenshots/` 下 11 张 webp，提交前建议转成 PNG/JPG（多数商店后台不接受 webp），并配一句英文说明，例如：
  - 主界面与文件列表
  - 播放页与弹幕效果
  - 电影 / 电视剧详情
  - 网络存储添加
  - 设置页

**6. 描述写作中已规避的点**
未引用任何第三方视频平台名称（弹幕统一写作 danmaku / 弹幕），未出现「免费下载」「破解」等易被机审拦截的措辞，也未承诺「支持全部格式」这类绝对化表述——杜比视界、DTS-HD 都加了「需要设备硬件支持」的前置说明。
