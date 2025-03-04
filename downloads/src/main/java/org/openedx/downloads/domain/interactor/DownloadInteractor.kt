package org.openedx.downloads.domain.interactor

import org.openedx.downloads.data.repository.DownloadRepository

class DownloadInteractor(
    private val repository: DownloadRepository
) {
    fun getDownloadCoursesPreview(refresh: Boolean) = repository.getDownloadCoursesPreview(refresh)

    fun getDownloadModels() = repository.getDownloadModels()

    suspend fun getAllDownloadModels() = repository.getAllDownloadModels()

    suspend fun getCourseStructure(courseId: String) = repository.getCourseStructure(courseId)
}
