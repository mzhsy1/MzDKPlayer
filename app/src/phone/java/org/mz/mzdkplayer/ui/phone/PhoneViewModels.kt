package org.mz.mzdkplayer.ui.phone

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.di.viewModelWithFactory
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.viewmodel.FTPConViewModel
import org.mz.mzdkplayer.viewmodel.FTPListViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkConViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkListViewModel
import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.NFSConViewModel
import org.mz.mzdkplayer.viewmodel.NFSListViewModel
import org.mz.mzdkplayer.viewmodel.SMBConViewModel
import org.mz.mzdkplayer.viewmodel.SMBListViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.viewmodel.WebDavConViewModel
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel

/**
 * 手机端全套 ViewModel（原写在 `PhoneRoot` 里的十几行装配，只挪位置）。
 *
 * 全部挂在 **Activity 作用域**（`viewModel()` 写在 `NavHost` 之外）：
 * - 五个协议的两类 VM：在目录之间来回跳、切到设置再切回来都不会重新建立连接
 *   （List = 连接增删（SharedPreferences），Con = 连接与列目录）；
 * - 刮削 / 媒体库：进目录、进匹配页、退回列表共用同一份状态，不会来回抖动；
 * - 音频元数据（`audio_cache`）与播放历史（`media_history`）：换歌 / 退出播放页回来不重建。
 */
internal class PhoneViewModels(
    val smbList: SMBListViewModel,
    val smbCon: SMBConViewModel,
    val ftpList: FTPListViewModel,
    val ftpCon: FTPConViewModel,
    val nfsList: NFSListViewModel,
    val nfsCon: NFSConViewModel,
    val webDavList: WebDavListViewModel,
    val webDavCon: WebDavConViewModel,
    val httpList: HTTPLinkListViewModel,
    val httpCon: HTTPLinkConViewModel,
    val movie: MovieViewModel,
    val mediaMeta: MediaMetaViewModel,
    val mediaLibrary: MediaLibraryViewModel,
    /** 各协议的自动刮削开关（电视端「设置 → 刮削与媒体库」里的那一组） */
    val settings: SettingsViewModel,
    val audio: AudioViewModel,
    val mediaHistory: MediaHistoryViewModel,
)

/** 每次重组新建的只是一个「持有 VM 引用的壳」，VM 本身由 `ViewModelStore` 保活 */
@Composable
internal fun rememberPhoneViewModels(): PhoneViewModels = PhoneViewModels(
    smbList = viewModel(),
    smbCon = viewModel(),
    ftpList = viewModel(),
    ftpCon = viewModel(),
    nfsList = viewModel(),
    nfsCon = viewModel(),
    webDavList = viewModel(),
    webDavCon = viewModel(),
    httpList = viewModel(),
    httpCon = viewModel(),
    movie = viewModelWithFactory { RepositoryProvider.createMovieViewModel() },
    mediaMeta = viewModelWithFactory { RepositoryProvider.createMediaMetaViewModel() },
    mediaLibrary = viewModelWithFactory { RepositoryProvider.createMediaLibraryViewModel() },
    settings = viewModel(),
    audio = viewModelWithFactory { RepositoryProvider.createAudioViewModel() },
    mediaHistory = viewModelWithFactory { RepositoryProvider.createMediaHistoryViewModel() },
)
