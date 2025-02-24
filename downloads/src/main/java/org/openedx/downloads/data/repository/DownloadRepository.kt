package org.openedx.downloads.data.repository

import kotlinx.coroutines.flow.flow
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.db.DownloadDao

class DownloadRepository(
    private val api: CourseApi,
    private val dao: DownloadDao,
    private val corePreferences: CorePreferences,
) {
    fun getDownloadCoursesPreview(refresh: Boolean) = flow {
        if (!refresh) {
            val cachedDownloadCoursesPreview = dao.getDownloadCoursesPreview()
            emit(cachedDownloadCoursesPreview.map { it.mapToDomain() })
        }
        val username = corePreferences.user?.username ?: ""
        val response = api.getDownloadCoursesPreview(username)
        val downloadCoursesPreview = response.map { it.mapToDomain() }
        val downloadCoursesPreviewEntity = response.map { it.mapToRoomEntity() }
        dao.insertDownloadCoursePreview(downloadCoursesPreviewEntity)
        emit(downloadCoursesPreview)
    }

}
