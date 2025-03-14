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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.DownloadCoursePreview
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
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureGot
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.DiscoveryNotifier
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
    private val discoveryNotifier: DiscoveryNotifier,
    private val courseNotifier: CourseNotifier,
    private val router: DownloadsRouter,
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
    val uiState: StateFlow<DownloadsUIState> = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage> = _uiMessage.asSharedFlow()

    private val courseBlockIds = mutableMapOf<String, List<String>>()

    val hasInternetConnection: Boolean get() = networkConnection.isOnline()

    private var downloadJobs = mutableMapOf<String, Job>()

    init {
        fetchDownloads(refresh = false)
        observeCourseDashboardUpdates()
        observeDownloadingModels()
        observeDownloadModelsStatus()
        observeCourseStructureUpdates()
    }

    private fun observeCourseDashboardUpdates() {
        viewModelScope.launch {
            discoveryNotifier.notifier.collect { notifier ->
                if (notifier is CourseDashboardUpdate) {
                    fetchDownloads(refresh = true)
                }
            }
        }
    }

    private fun observeCourseStructureUpdates() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { notifier ->
                when (notifier) {
                    is CourseStructureGot -> {
                        fetchDownloads(refresh = true)
                    }

                    is CourseStructureUpdated -> {
                        fetchDownloads(refresh = true)
                    }
                }
            }
        }
    }

    private fun observeDownloadingModels() {
        viewModelScope.launch {
            downloadingModelsFlow.collect { downloadModels ->
                _uiState.update { state ->
                    state.copy(downloadModels = downloadModels)
                }
            }
        }
    }

    private fun observeDownloadModelsStatus() {
        viewModelScope.launch {
            downloadModelsStatusFlow.collect { statusMap ->
                val updatedCourseStates = courseBlockIds.mapValues { (courseId, blockIds) ->
                    val currentCourseState = uiState.value.courseDownloadState[courseId]
                    val blockStates = blockIds.mapNotNull { statusMap[it] }
                    val computedState = if (blockStates.isEmpty()) {
                        DownloadedState.NOT_DOWNLOADED
                    } else {
                        determineCourseState(blockStates)
                    }
                    if (currentCourseState == DownloadedState.LOADING_COURSE_STRUCTURE &&
                        computedState == DownloadedState.NOT_DOWNLOADED
                    ) {
                        DownloadedState.LOADING_COURSE_STRUCTURE
                    } else {
                        computedState
                    }
                }

                _uiState.update { state ->
                    state.copy(courseDownloadState = updatedCourseStates)
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
            updateLoadingState(isLoading = !refresh, isRefreshing = refresh)
            interactor.getDownloadCoursesPreview(refresh)
                .onCompletion { resetLoadingState() }
                .catch { e ->
                    emitErrorMessage(e)
                }
                .collect { downloadCoursePreviews ->
                    downloadCoursePreviews.forEach { preview ->
                        runCatching { initializeCourseBlocks(preview.id, useCache = true) }
                            .onFailure { it.printStackTrace() }
                    }
                    allBlocks.values
                        .filter { it.type == BlockType.SEQUENTIAL }
                        .forEach { addDownloadableChildrenForSequentialBlock(it) }
                    initDownloadModelsStatus()
                    _uiState.update { state ->
                        state.copy(downloadCoursePreviews = downloadCoursePreviews)
                    }
                }
        }
    }

    private fun updateLoadingState(isLoading: Boolean, isRefreshing: Boolean) {
        _uiState.update { state ->
            state.copy(isLoading = isLoading, isRefreshing = isRefreshing)
        }
    }

    private fun resetLoadingState() {
        _uiState.update { state ->
            state.copy(isLoading = false, isRefreshing = false)
        }
    }

    private suspend fun emitErrorMessage(e: Throwable) {
        val text = if (e.isInternetError()) {
            R.string.core_error_no_connection
        } else {
            R.string.core_error_unknown_error
        }
        _uiMessage.emit(
            UIMessage.SnackBarMessage(resourceManager.getString(text))
        )
    }

    fun refreshData() {
        _uiState.update { it.copy(isRefreshing = true) }
        fetchDownloads(refresh = true)
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        downloadsRouter.navigateToSettings(fragmentManager)
    }

    fun downloadCourse(fragmentManager: FragmentManager, courseId: String) {
        logEvent(DownloadsAnalyticsEvent.DOWNLOAD_COURSE_CLICKED)
        try {
            showDownloadPopup(fragmentManager, courseId)
        } catch (e: Exception) {
            logEvent(DownloadsAnalyticsEvent.DOWNLOAD_ERROR)
            updateCourseState(courseId, DownloadedState.NOT_DOWNLOADED)
            viewModelScope.launch {
                emitErrorMessage(e)
            }
        }
    }

    fun cancelDownloading(courseId: String) {
        logEvent(DownloadsAnalyticsEvent.CANCEL_DOWNLOAD_CLICKED)
        viewModelScope.launch {
            downloadJobs[courseId]?.cancel()
            interactor.getAllDownloadModels()
                .filter { it.courseId == courseId && it.downloadedState.isWaitingOrDownloading }
                .forEach { removeBlockDownloadModel(it.id) }
        }
    }

    fun removeDownloads(fragmentManager: FragmentManager, courseId: String) {
        logEvent(DownloadsAnalyticsEvent.REMOVE_DOWNLOAD_CLICKED)
        viewModelScope.launch {
            val downloadModels = interactor.getDownloadModels().first().filter {
                it.courseId == courseId
            }
            val downloadedModels = downloadModels.filter {
                it.downloadedState == DownloadedState.DOWNLOADED
            }
            val totalSize = downloadedModels.sumOf { it.size }
            val title = getCoursePreview(courseId)?.name.orEmpty()
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

    private suspend fun initializeCourseBlocks(
        courseId: String,
        useCache: Boolean
    ): CourseStructure {
        val courseStructure = if (useCache) {
            interactor.getCourseStructureFromCache(courseId)
        } else {
            interactor.getCourseStructure(courseId)
        }
        courseBlockIds[courseStructure.id] = courseStructure.blockData.map { it.id }
        addBlocks(courseStructure.blockData)
        return courseStructure
    }

    private fun showDownloadPopup(fragmentManager: FragmentManager, courseId: String) {
        val coursePreview = getCoursePreview(courseId) ?: return
        downloadDialogManager.showPopup(
            coursePreview = coursePreview,
            isBlocksDownloaded = false,
            fragmentManager = fragmentManager,
            removeDownloadModels = ::removeDownloadModels,
            saveDownloadModels = {
                initiateSaveDownloadModels(courseId)
            },
            onDismissClick = {
                logEvent(DownloadsAnalyticsEvent.DOWNLOAD_CANCELLED)
                updateCourseState(courseId, DownloadedState.NOT_DOWNLOADED)
            },
            onConfirmClick = {
                logEvent(DownloadsAnalyticsEvent.DOWNLOAD_CONFIRMED)
            }
        )
    }

    private fun initiateSaveDownloadModels(courseId: String) {
        downloadJobs[courseId] = viewModelScope.launch {
            try {
                updateCourseState(courseId, DownloadedState.LOADING_COURSE_STRUCTURE)
                val courseStructure = initializeCourseBlocks(courseId, useCache = false)
                courseStructure.blockData
                    .filter { it.type == BlockType.SEQUENTIAL }
                    .forEach { sequentialBlock ->
                        addDownloadableChildrenForSequentialBlock(sequentialBlock)
                        super.saveDownloadModels(
                            fileUtil.getExternalAppDir().path,
                            courseId,
                            sequentialBlock.id
                        )
                    }
            } catch (e: Exception) {
                updateCourseState(courseId, DownloadedState.NOT_DOWNLOADED)
                emitErrorMessage(e)
            }
        }
    }

    fun navigateToCourseOutline(fm: FragmentManager, courseId: String) {
        val coursePreview = getCoursePreview(courseId) ?: return
        router.navigateToCourseOutline(
            fm = fm,
            courseId = coursePreview.id,
            courseTitle = coursePreview.name,
        )
    }

    private fun logEvent(event: DownloadsAnalyticsEvent) {
        analytics.logEvent(
            event = event.eventName,
            params = mapOf(DownloadsAnalyticsKey.NAME.key to event.biValue)
        )
    }

    private fun updateCourseState(courseId: String, state: DownloadedState) {
        _uiState.update { currentState ->
            currentState.copy(
                courseDownloadState = currentState.courseDownloadState.toMutableMap().apply {
                    put(courseId, state)
                }
            )
        }
    }

    private fun getCoursePreview(courseId: String): DownloadCoursePreview? {
        return _uiState.value.downloadCoursePreviews.find { it.id == courseId }
    }
}

interface DownloadsViewActions {
    object OpenSettings : DownloadsViewActions
    object SwipeRefresh : DownloadsViewActions
    data class OpenCourse(val courseId: String) : DownloadsViewActions
    data class DownloadCourse(val courseId: String) : DownloadsViewActions
    data class CancelDownloading(val courseId: String) : DownloadsViewActions
    data class RemoveDownloads(val courseId: String) : DownloadsViewActions
}
