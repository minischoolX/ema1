package org.openedx.downloads.presentation.dates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
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
                } else if (uiState.courses.isEmpty()) {
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
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {

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
