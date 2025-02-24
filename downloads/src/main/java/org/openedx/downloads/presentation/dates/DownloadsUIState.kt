package org.openedx.downloads.presentation.dates

import org.openedx.core.domain.model.DownloadCoursePreview

data class DownloadsUIState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val downloadCoursePreviews: List<DownloadCoursePreview> = emptyList()
)
