package org.mz.mzdkplayer.ui.screen.setting

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.viewmodel.PerformanceTestViewModel

@Composable
fun PerformanceTestScreen(
    viewModel: PerformanceTestViewModel = viewModelWithFactory {
        RepositoryProvider.createPerformanceTestViewModel()
    }
) {
    val status by viewModel.status.collectAsState()
    val isInserting by viewModel.isInserting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isInserting) {
            LoadingScreen(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 插入数据按钮 ---
            MyIconButton(
                text = stringResource(R.string.ui_label_insert_50000_dummy_records),
                icon = R.drawable.baseline_search_24,
                onClick = { viewModel.startInsertion(50000) },
                enabled = !isInserting,
                modifier = Modifier.weight(1f)
            )

            // --- 清理数据库按钮 ---
            MyIconButton(
                text = stringResource(R.string.ui_label_clear_all_cached_data),
                icon = R.drawable.delete24dp,
                onClick = { viewModel.clearDatabase() },
                enabled = !isInserting,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 状态显示 ---
        Text(
            text = stringResource(R.string.ui_label_current_status, status),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
