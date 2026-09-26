package org.mz.mzdkplayer.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TMDBConfigDialog(
    settingsVM: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val state by settingsVM.uiState.collectAsState()
    val testResult by settingsVM.tmdbTestResult.collectAsState()
    
    var customUrl by remember { mutableStateOf(state.tmdbBaseUrl) }
    
    val commonMirrors = listOf(
        SettingsRepository.DEFAULT_TMDB_URL,
        "https://api.tmdb.org/3/",
        "https://tmdb.org/3/",
        "https://tmdb.movie/3/"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.setting_tmdb_api_mirror),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.weight(1f)) {
                    // 左侧：预设列表
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(commonMirrors) { mirror ->
                            ListItem(
                                selected = customUrl == mirror,
                                onClick = { customUrl = mirror },
                                headlineContent = { Text(if(mirror == SettingsRepository.DEFAULT_TMDB_URL) "Official" else mirror) },
                                colors = myListItemCoverColor()
                            )
                        }
                    }

                    Spacer(Modifier.width(24.dp))

                    // 右侧：自定义输入与测试
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ui_label_custom_tmdb_url),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.LightGray
                        )
                        
                        Spacer(Modifier.height(8.dp))

                        BasicTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            decorationBox = { innerTextField ->
                                if (customUrl.isEmpty()) {
                                    Text("https://...", color = Color.Gray)
                                }
                                innerTextField()
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        MyIconButton(
                            text = stringResource(R.string.ui_label_test_tmdb_connection),
                            icon = R.drawable.check24dp,
                            onClick = { settingsVM.testTmdbConnection(customUrl) }
                        )

                        Spacer(Modifier.height(8.dp))

                        when (val res = testResult) {
                            is Resource.Loading -> Text("Testing...", color = Color.Cyan)
                            is Resource.Success -> Text(stringResource(R.string.ui_label_tmdb_connection_success), color = Color.Green)
                            is Resource.Error -> Text(stringResource(R.string.ui_label_tmdb_connection_failed, res.message), color = Color.Red)
                            else -> {}
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MyIconButton(
                        text = stringResource(R.string.ui_label_cancel),
                        icon = R.drawable.close24dp,
                        onClick = {
                            settingsVM.clearTmdbTestResult()
                            onDismiss()
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    MyIconButton(
                        text = stringResource(R.string.ui_label_save_connection),
                        icon = R.drawable.check24dp,
                        onClick = {
                            settingsVM.setTmdbBaseUrl(customUrl)
                            settingsVM.clearTmdbTestResult()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
