package org.openedx.downloads.presentation.dates

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.downloads.domain.interactor.DownloadInteractor
import org.openedx.downloads.presentation.DownloadsRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class DownloadsViewModel(
    private val downloadsRouter: DownloadsRouter,
    private val networkConnection: NetworkConnection,
    private val interactor: DownloadInteractor,
    private val resourceManager: ResourceManager,
    private val config: Config,
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableStateFlow(DownloadsUIState())
    val uiState: StateFlow<DownloadsUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()


    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        fetchDates(false)
    }

    private fun fetchDates(refresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { state ->
                state.copy(
                    isLoading = !refresh,
                    isRefreshing = refresh
                )
            }
            interactor.getDownloadCoursesPreview(refresh)
                .onCompletion {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                .catch { e ->
                    if (e.isInternetError()) {
                        _uiMessage.emit(
                            UIMessage.SnackBarMessage(
                                resourceManager.getString(R.string.core_error_no_connection)
                            )
                        )
                    } else {
                        _uiMessage.emit(
                            UIMessage.SnackBarMessage(
                                resourceManager.getString(R.string.core_error_unknown_error)
                            )
                        )
                    }
                }
                .collect { downloadCoursePreviews ->
                    _uiState.update { state ->
                        state.copy(
                            downloadCoursePreviews = downloadCoursePreviews
                        )
                    }
                }
        }
    }

    fun refreshData() {
        _uiState.update { state ->
            state.copy(
                isRefreshing = true
            )
        }
        fetchDates(true)
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        downloadsRouter.navigateToSettings(fragmentManager)
    }
}

interface DownloadsViewActions {
    object OpenSettings : DownloadsViewActions
    object SwipeRefresh : DownloadsViewActions
}
