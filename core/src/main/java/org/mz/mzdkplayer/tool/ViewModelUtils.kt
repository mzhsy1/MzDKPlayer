package org.mz.mzdkplayer.tool

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
inline fun <reified VM : ViewModel> viewModelWithFactory(
    crossinline factory: () -> VM
): VM {
    val storeOwner = LocalViewModelStoreOwner.current
        ?: throw IllegalStateException("ViewModelStoreOwner not found")

    return ViewModelProvider(
        storeOwner,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return factory() as T
            }
        }
    )[VM::class.java]
}