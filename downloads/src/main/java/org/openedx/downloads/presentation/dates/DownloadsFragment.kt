package org.openedx.downloads.presentation.dates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXDropdownMenuItem
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.crop
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.downloads.R
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue

class DownloadsFragment : Fragment() {

    private val viewModel by viewModel<DownloadsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                DownloadsScreen(
                    uiState = uiState,
                    uiMessage = uiMessage,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onAction = { action ->
                        when (action) {
                            DownloadsViewActions.OpenSettings -> {
                                viewModel.onSettingsClick(requireActivity().supportFragmentManager)
                            }

                            DownloadsViewActions.SwipeRefresh -> {
                                viewModel.refreshData()
                            }
                        }
                    }
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DownloadsScreen(
    uiState: DownloadsUIState,
    uiMessage: UIMessage?,
    hasInternetConnection: Boolean,
    onAction: (DownloadsViewActions) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val windowSize = rememberWindowSize()
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth(),
            )
        )
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { onAction(DownloadsViewActions.SwipeRefresh) }
    )
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background,
        topBar = {
            Toolbar(
                modifier = Modifier
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                label = stringResource(id = R.string.downloads),
                canShowSettingsIcon = true,
                onSettingsClick = {
                    onAction(DownloadsViewActions.OpenSettings)
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                    }
                } else if (false) {
                    EmptyState()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutForLandscape()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            modifier = contentWidth,
                            contentPadding = PaddingValues(bottom = 20.dp, top = 12.dp)
                        ) {
                            item {
                                CourseItem(
                                    apiHostUrl = "",
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

                PullRefreshIndicator(
                    uiState.isRefreshing,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )

                if (!isInternetConnectionShown && !hasInternetConnection) {
                    OfflineModeDialog(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        onDismissCLick = {
                            isInternetConnectionShown = true
                        },
                        onReloadClick = {
                            isInternetConnectionShown = true
                            onAction(DownloadsViewActions.SwipeRefresh)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun CourseItem(
    modifier: Modifier = Modifier,
    apiHostUrl: String,
    onClick: () -> Unit,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val progress: Float = try {
        1.toFloat() / 2.toFloat()
    } catch (_: ArithmeticException) {
        0f
    }
    Card(
        modifier = modifier
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 4.dp,
    ) {
        Box {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
//                        .data(course.course.courseImage.toImageLink(apiHostUrl))
                        .error(org.openedx.core.R.drawable.core_no_image_course)
                        .placeholder(org.openedx.core.R.drawable.core_no_image_course)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                ) {
                    Text(
                        text = "course.name",
                        style = MaterialTheme.appTypography.titleLarge,
                        color = MaterialTheme.appColors.textDark,
                        overflow = TextOverflow.Ellipsis,
                        minLines = 1,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        progress = progress,
                        color = MaterialTheme.appColors.successGreen,
                        backgroundColor = MaterialTheme.appColors.divider
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    IconText(
                        icon = Icons.Filled.CloudDone,
                        color = MaterialTheme.appColors.successGreen,
                        text = "qwe"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    IconText(
                        icon = Icons.Outlined.CloudDownload,
                        color = MaterialTheme.appColors.textPrimaryVariant,
                        text = "rty"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OpenEdXButton(
                        onClick = onClick,
                        content = {
                            IconText(
                                text = stringResource(R.string.downloads_download_course),
                                icon = Icons.Outlined.CloudDownload,
                                color = MaterialTheme.appColors.primaryButtonText,
                                textStyle = MaterialTheme.appTypography.labelLarge
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd),
            ) {
                MoreButton(
                    onClick = {
                        isDropdownExpanded = true
                    }
                )
                DropdownMenu(
                    modifier = Modifier
                        .crop(vertical = 8.dp)
                        .defaultMinSize(minWidth = 269.dp)
                        .background(MaterialTheme.appColors.background),
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                ) {
                    Column {
                        OpenEdXDropdownMenuItem(
                            text = "Dropdown option1",
                            onClick = {}
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.appColors.divider
                        )
                        OpenEdXDropdownMenuItem(
                            text = "Dropdown option2",
                            onClick = {}
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun MoreButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = MaterialTheme.appColors.onPrimary.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(4.dp),
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = null,
            tint = MaterialTheme.appColors.onSurface
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
//    Box(
//        modifier = modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            modifier = Modifier.width(200.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                painter = painterResource(id = org.openedx.core.R.drawable.core_ic_book),
//                tint = MaterialTheme.appColors.textFieldBorder,
//                contentDescription = null
//            )
//            Spacer(Modifier.height(4.dp))
//            Text(
//                modifier = Modifier
//                    .testTag("txt_empty_state_title")
//                    .fillMaxWidth(),
//                text = stringResource(id = R.string.dates_empty_state_title),
//                color = MaterialTheme.appColors.textDark,
//                style = MaterialTheme.appTypography.titleMedium,
//                textAlign = TextAlign.Center
//            )
//            Spacer(Modifier.height(12.dp))
//            Text(
//                modifier = Modifier
//                    .testTag("txt_empty_state_description")
//                    .fillMaxWidth(),
//                text = stringResource(id = R.string.dates_empty_state_description),
//                color = MaterialTheme.appColors.textDark,
//                style = MaterialTheme.appTypography.labelMedium,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
}

@Preview
@Composable
private fun DatesScreenPreview() {
    OpenEdXTheme {
        DownloadsScreen(
            uiState = DownloadsUIState(isLoading = false),
            uiMessage = null,
            hasInternetConnection = true,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun CourseItemPreview() {
    OpenEdXTheme {
        CourseItem(
            apiHostUrl = "",
            onClick = {}
        )
    }
}
