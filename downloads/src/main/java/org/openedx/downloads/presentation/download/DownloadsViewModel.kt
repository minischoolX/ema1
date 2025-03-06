package org.openedx.downloads.presentation.download

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.DownloadsAnalytics
import org.openedx.core.presentation.DownloadsAnalyticsEvent
import org.openedx.core.presentation.DownloadsAnalyticsKey
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogItem
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.downloads.domain.interactor.DownloadInteractor
import org.openedx.downloads.presentation.DownloadsRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil

class DownloadsViewModel(
    private val downloadsRouter: DownloadsRouter,
    private val networkConnection: NetworkConnection,
    private val interactor: DownloadInteractor,
    private val downloadDialogManager: DownloadDialogManager,
    private val resourceManager: ResourceManager,
    private val fileUtil: FileUtil,
    private val config: Config,
    private val analytics: DownloadsAnalytics,
    preferencesManager: CorePreferences,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
    downloadHelper: DownloadHelper,
) : BaseDownloadViewModel(
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics,
    downloadHelper,
) {
    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableStateFlow(DownloadsUIState())
    val uiState: StateFlow<DownloadsUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val blockIdsByCourseId = mutableMapOf<String, List<String>>()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private var downloadJobs = mutableMapOf<String, Job>()

    init {
        fetchDownloads(false)

        viewModelScope.launch {
            downloadingModelsFlow.collect { downloadModels ->
                _uiState.update { state ->
                    state.copy(downloadModels = downloadModels)
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect { statusMap ->
                val downloadingCourseState = blockIdsByCourseId
                    .mapValues { (courseId, blockIds) ->
                        val currentCourseState = uiState.value.courseDownloadState[courseId]
                        val blockStates = blockIds.mapNotNull { statusMap[it] }
                        val courseDownloadState = if (blockStates.isEmpty()) {
                            DownloadedState.NOT_DOWNLOADED
                        } else {
                            determineCourseState(blockStates)
                        }
                        val isLoadingCourseStructure =
                            currentCourseState == DownloadedState.LOADING_COURSE_STRUCTURE &&
                                    courseDownloadState == DownloadedState.NOT_DOWNLOADED
                        if (isLoadingCourseStructure) {
                            DownloadedState.LOADING_COURSE_STRUCTURE
                        } else {
                            courseDownloadState
                        }
                    }

                _uiState.update { state ->
                    state.copy(courseDownloadState = downloadingCourseState)
                }
            }
        }
    }

    private fun determineCourseState(blockStates: List<DownloadedState>): DownloadedState {
        return when {
            blockStates.all { it == DownloadedState.DOWNLOADED } -> DownloadedState.DOWNLOADED
            blockStates.all { it == DownloadedState.WAITING } -> DownloadedState.WAITING
            blockStates.any { it == DownloadedState.DOWNLOADING } -> DownloadedState.DOWNLOADING
            else -> DownloadedState.NOT_DOWNLOADED
        }
    }

    private fun fetchDownloads(refresh: Boolean) {
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
                    downloadCoursePreviews.map {
                        try {
                            initBlocks(it.id, true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val subSectionsBlocks =
                        allBlocks.values.filter { it.type == BlockType.SEQUENTIAL }
                    subSectionsBlocks.map { subSection ->
                        addDownloadableChildrenForSequentialBlock(subSection)
                    }
                    initDownloadModelsStatus()
                    _uiState.update { state ->
                        state.copy(
                            downloadCoursePreviews = downloadCoursePreviews,
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
        fetchDownloads(true)
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        downloadsRouter.navigateToSettings(fragmentManager)
    }

    fun downloadCourse(fragmentManager: FragmentManager, courseId: String) {
        logEvent(DownloadsAnalyticsEvent.DOWNLOAD_COURSE_CLICKED)
        downloadJobs[courseId] = viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(
                        courseDownloadState = state.courseDownloadState.toMap() +
                                (courseId to DownloadedState.LOADING_COURSE_STRUCTURE)
                    )
                }
                downloadAllBlocks(fragmentManager, courseId)
            } catch (e: Exception) {
                logEvent(DownloadsAnalyticsEvent.DOWNLOAD_ERROR)
                _uiState.update { state ->
                    state.copy(
                        courseDownloadState = state.courseDownloadState.toMap() +
                                (courseId to DownloadedState.NOT_DOWNLOADED)
                    )
                }
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
        }
    }

    fun cancelDownloading(courseId: String) {
        logEvent(DownloadsAnalyticsEvent.CANCEL_DOWNLOAD_CLICKED)
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    courseDownloadState = state.courseDownloadState.toMap() +
                            (courseId to DownloadedState.NOT_DOWNLOADED)
                )
            }
            downloadJobs[courseId]?.cancel()
            interactor.getAllDownloadModels()
                .filter { it.courseId == courseId && it.downloadedState.isWaitingOrDownloading }
                .forEach { removeBlockDownloadModel(it.id) }
        }
    }

    fun removeDownloads(fragmentManager: FragmentManager, courseId: String) {
        logEvent(DownloadsAnalyticsEvent.REMOVE_DOWNLOAD_CLICKED)
        viewModelScope.launch {
            val downloadModels =
                interactor.getDownloadModels().first().filter { it.courseId == courseId }
            val totalSize = downloadModels.sumOf { it.size }
            val title = _uiState.value.downloadCoursePreviews.find { it.id == courseId }?.name ?: ""
            val downloadDialogItem = DownloadDialogItem(
                title = title,
                size = totalSize,
                icon = Icons.AutoMirrored.Outlined.InsertDriveFile
            )
            downloadDialogManager.showRemoveDownloadModelPopup(
                downloadDialogItem = downloadDialogItem,
                fragmentManager = fragmentManager,
                removeDownloadModels = {
                    downloadModels.forEach { super.removeBlockDownloadModel(it.id) }
                    logEvent(DownloadsAnalyticsEvent.DOWNLOAD_REMOVED)
                }
            )
        }
    }

    private suspend fun initBlocks(courseId: String, cached: Boolean): CourseStructure {
        val courseStructure = if (cached) {
            interactor.getCourseStructureFromCache(courseId)
        } else {
            interactor.getCourseStructure(courseId)
        }
        blockIdsByCourseId[courseStructure.id] = courseStructure.blockData.map { it.id }
        addBlocks(courseStructure.blockData)
        return courseStructure
    }

    private suspend fun downloadAllBlocks(fragmentManager: FragmentManager, courseId: String) {
        val courseStructure = initBlocks(courseId, false)
        val downloadModels = interactor.getDownloadModels()
            .map { list -> list.filter { it.courseId in courseId } }
            .first()
        val subSectionsBlocks = allBlocks.values.filter { it.type == BlockType.SEQUENTIAL }
        val notDownloadedSubSectionBlocks = subSectionsBlocks.mapNotNull { subSection ->
            addDownloadableChildrenForSequentialBlock(subSection)
            val verticalBlocks = allBlocks.values.filter { it.id in subSection.descendants }
            val notDownloadedBlocks = courseStructure.blockData.filter { block ->
                block.id in verticalBlocks.flatMap { it.descendants } &&
                        block.isDownloadable &&
                        downloadModels.none { it.id == block.id }
            }
            if (notDownloadedBlocks.isNotEmpty()) subSection else null
        }
        downloadDialogManager.showPopup(
            subSectionsBlocks = notDownloadedSubSectionBlocks,
            courseId = courseId,
            isBlocksDownloaded = false,
            fragmentManager = fragmentManager,
            removeDownloadModels = ::removeDownloadModels,
            saveDownloadModels = { blockId ->
                saveDownloadModels(fileUtil.getExternalAppDir().path, courseId, blockId)
            },
            onDismissClick = {
                logEvent(DownloadsAnalyticsEvent.DOWNLOAD_CANCELLED)
                _uiState.update { state ->
                    state.copy(
                        courseDownloadState = state.courseDownloadState.toMap() +
                                (courseId to DownloadedState.NOT_DOWNLOADED)
                    )
                }
            },
            onConfirmClick = {
                logEvent(DownloadsAnalyticsEvent.DOWNLOAD_CONFIRMED)
            }
        )
    }

    fun logEvent(event: DownloadsAnalyticsEvent) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(DownloadsAnalyticsKey.NAME.key, event.biValue)
            }
        )
    }
}

interface DownloadsViewActions {
    object OpenSettings : DownloadsViewActions
    object SwipeRefresh : DownloadsViewActions
    data class DownloadCourse(val courseId: String) : DownloadsViewActions
    data class CancelDownloading(val courseId: String) : DownloadsViewActions
    data class RemoveDownloads(val courseId: String) : DownloadsViewActions
}
