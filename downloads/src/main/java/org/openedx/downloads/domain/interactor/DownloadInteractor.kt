package org.openedx.downloads.domain.interactor

import org.openedx.downloads.data.repository.DownloadRepository

class DownloadInteractor(
    private val repository: DownloadRepository
) {
    fun getDownloadCoursesPreview(refresh: Boolean) = repository.getDownloadCoursesPreview(refresh)
}
