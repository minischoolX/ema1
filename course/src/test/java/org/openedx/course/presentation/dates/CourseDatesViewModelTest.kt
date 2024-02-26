package org.openedx.course.presentation.dates

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.data.model.DateType
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.calendarsync.CalendarManager
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDatesViewModelTest {
    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val notifier = mockk<CourseNotifier>()
    private val interactor = mockk<CourseInteractor>()
    private val calendarManager = mockk<CalendarManager>()
    private val networkConnection = mockk<NetworkConnection>()
    private val corePreferences = mockk<CorePreferences>()

    private val openEdx = "OpenEdx"
    private val calendarTitle = "OpenEdx - Abc"
    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val user = User(
        id = 0,
        username = "",
        email = "",
        name = "",
    )
    private val appConfig = AppConfig(
        CourseDatesCalendarSync(
            isEnabled = true,
            isSelfPacedEnabled = true,
            isInstructorPacedEnabled = true,
            isDeepLinkEnabled = false,
        )
    )
    private val dateBlock = CourseDateBlock(
        complete = false,
        date = Date(),
        dateType = DateType.TODAY_DATE,
        description = "Mocked Course Date Description"
    )
    private val mockDateBlocks = linkedMapOf(
        Pair(
            DatesSection.COMPLETED,
            listOf(dateBlock, dateBlock)
        ),
        Pair(
            DatesSection.PAST_DUE,
            listOf(dateBlock, dateBlock)
        ),
        Pair(
            DatesSection.TODAY,
            listOf(dateBlock, dateBlock)
        )
    )
    private val mockCourseDatesBannerInfo = CourseDatesBannerInfo(
        missedDeadlines = true,
        missedGatedContent = false,
        verifiedUpgradeLink = "",
        contentTypeGatingEnabled = false,
        hasEnded = true,
    )
    private val mockedCourseDatesResult = CourseDatesResult(
        datesSection = mockDateBlocks,
        courseBanner = mockCourseDatesBannerInfo,
    )
    private val courseStructure = CourseStructure(
        root = "",
        blockData = listOf(),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(0),
        startDisplay = "",
        startType = "",
        end = null,
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        certificate = null,
        isSelfPaced = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(id = R.string.platform_name) } returns openEdx
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { corePreferences.user } returns user
        every { corePreferences.appConfig } returns appConfig
        every { notifier.notifier } returns emptyFlow()
        every { calendarManager.getCourseCalendarTitle(any()) } returns calendarTitle
        coEvery { notifier.send(any<CreateCalendarSyncEvent>()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseDates no internet connection exception`() = runTest {
        val viewModel = CourseDatesViewModel(
            "",
            "",
            true,
            notifier,
            interactor,
            calendarManager,
            networkConnection,
            resourceManager,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        Assert.assertEquals(noInternet, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Loading)
    }

    @Test
    fun `getCourseDates unknown exception`() = runTest {
        val viewModel = CourseDatesViewModel(
            "",
            "",
            true,
            notifier,
            interactor,
            calendarManager,
            networkConnection,
            resourceManager,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        Assert.assertEquals(somethingWrong, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Loading)
    }

    @Test
    fun `getCourseDates success with internet`() = runTest {
        val viewModel = CourseDatesViewModel(
            "",
            "",
            true,
            notifier,
            interactor,
            calendarManager,
            networkConnection,
            resourceManager,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } returns mockedCourseDatesResult

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Dates)
    }

    @Test
    fun `getCourseDates success with EmptyList`() = runTest {
        val viewModel = CourseDatesViewModel(
            "",
            "",
            true,
            notifier,
            interactor,
            calendarManager,
            networkConnection,
            resourceManager,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } returns CourseDatesResult(
            datesSection = linkedMapOf(),
            courseBanner = mockCourseDatesBannerInfo,
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Empty)
    }
}
