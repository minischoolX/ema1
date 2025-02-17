package org.openedx.downloads.presentation.dates

data class DownloadsUIState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val courses: List<Any> = emptyList()
)
