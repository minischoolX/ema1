package org.openedx.discussion.presentation.topics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.RefreshDiscussions
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionRouter

class DiscussionTopicsViewModel(
    val courseId: String,
    private val courseTitle: String,
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: DiscussionAnalytics,
    private val courseNotifier: CourseNotifier,
    val discussionRouter: DiscussionRouter,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DiscussionTopicsUIState>()
    val uiState: LiveData<DiscussionTopicsUIState>
        get() = _uiState

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    init {
        collectCourseNotifier()

        getCourseTopic()
    }

    private fun getCourseTopic() {
        viewModelScope.launch {
            try {
                val response = interactor.getCourseTopics(courseId)
                _uiState.value = DiscussionTopicsUIState.Topics(response)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            } finally {
                courseNotifier.send(CourseLoading(false))
            }
        }
    }

    fun discussionClickedEvent(action: String, data: String, title: String) {
        when (action) {
            ALL_POSTS -> {
                analytics.discussionAllPostsClickedEvent(courseId, courseTitle)
            }

            FOLLOWING_POSTS -> {
                analytics.discussionFollowingClickedEvent(courseId, courseTitle)
            }

            TOPIC -> {
                analytics.discussionTopicClickedEvent(courseId, courseTitle, data, title)
            }
        }
    }

    private fun collectCourseNotifier() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is RefreshDiscussions -> getCourseTopic()
                }
            }
        }
    }

    companion object DiscussionTopic {
        const val TOPIC = "Topic"
        const val ALL_POSTS = "All posts"
        const val FOLLOWING_POSTS = "Following"
    }
}