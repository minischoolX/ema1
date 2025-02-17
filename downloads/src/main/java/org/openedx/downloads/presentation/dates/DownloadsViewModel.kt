package org.openedx.downloads.presentation.dates

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.downloads.presentation.DownloadsRouter
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage

class DownloadsViewModel(
    private val downloadsRouter: DownloadsRouter,
    private val networkConnection: NetworkConnection,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUIState())
    val uiState: StateFlow<DownloadsUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()


    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        fetchDates()
    }

    private fun fetchDates() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun refreshData() {
        _uiState.update { state ->
            state.copy(
                isRefreshing = true
            )
        }
        fetchDates()
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        downloadsRouter.navigateToSettings(fragmentManager)
    }
}

interface DownloadsViewActions {
    object OpenSettings : DownloadsViewActions
    object SwipeRefresh : DownloadsViewActions
}
