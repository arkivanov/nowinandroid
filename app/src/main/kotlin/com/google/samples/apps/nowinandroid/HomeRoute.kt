package com.google.samples.apps.nowinandroid

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.google.samples.apps.nowinandroid.HomeComponent.Child.Bookmarks
import com.google.samples.apps.nowinandroid.HomeComponent.Child.ForYou
import com.google.samples.apps.nowinandroid.HomeComponent.Child.Interests
import com.google.samples.apps.nowinandroid.R.string
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaGradientBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopAppBar
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.feature.bookmarks.BookmarksRoute
import com.google.samples.apps.nowinandroid.feature.foryou.ForYouRoute
import com.google.samples.apps.nowinandroid.feature.interests.InterestsRoute
import com.google.samples.apps.nowinandroid.navigation.TopLevelDestination
import com.google.samples.apps.nowinandroid.ui.NiaAppState
import com.google.samples.apps.nowinandroid.ui.NiaBottomBar
import com.google.samples.apps.nowinandroid.ui.NiaNavRail
import com.google.samples.apps.nowinandroid.feature.bookmarks.R as BookmarksR
import com.google.samples.apps.nowinandroid.feature.interests.R as InterestsR
import com.google.samples.apps.nowinandroid.feature.settings.R as SearchR
import com.google.samples.apps.nowinandroid.feature.settings.R as SettingsR

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeRoute(
    component: HomeComponent,
    windowSizeClass: WindowSizeClass,
    appState: NiaAppState,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTopicClick: (topicId: String) -> Unit,
    onShowSnackbar: suspend (message: String, action: String?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val unreadDestinations by appState.topLevelDestinationsWithUnreadResources.collectAsStateWithLifecycle()
    val isBottomBarVisible = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val isNavigationRailVisible = !isBottomBarVisible

    NiaScaffold(
        modifier = modifier,
        bottomBar = {
            if (isBottomBarVisible) {
                NiaBottomBar(
                    component = component,
                    destinationsWithUnreadResources = unreadDestinations,
                    modifier = Modifier.testTag("NiaBottomBar"),
                )
            }
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
        ) {
            if (isNavigationRailVisible) {
                NiaNavRail(
                    component = component,
                    destinationsWithUnreadResources = unreadDestinations,
                    modifier = Modifier
                        .testTag("NiaNavRail")
                        .safeDrawingPadding(),
                )
            }

            Children(
                component = component,
                onSettingsClick = onSettingsClick,
                onSearchClick = onSearchClick,
                onTopicClick = onTopicClick,
                onShowSnackbar = onShowSnackbar,
            )
        }
    }
}

@Composable
fun Children(
    component: HomeComponent,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTopicClick: (topicId: String) -> Unit,
    onShowSnackbar: suspend (message: String, action: String?) -> Boolean,
) {
    Children(
        stack = component.stack,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is ForYou ->
                NiaGradientBackground {
                    HomeChildRoute(
                        titleResId = string.app_name,
                        onSettingsClick = onSettingsClick,
                        onSearchClick = onSearchClick,
                    ) {
                        ForYouRoute(
                            viewModel = child.component,
                            onTopicClick = onTopicClick,
                        )
                    }
                }

            is Bookmarks ->
                HomeChildRoute(
                    titleResId = BookmarksR.string.saved,
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick,
                ) {
                    BookmarksRoute(
                        viewModel = child.component,
                        onTopicClick = onTopicClick,
                        onShowSnackbar = onShowSnackbar,
                    )
                }

            is Interests ->
                HomeChildRoute(
                    titleResId = InterestsR.string.interests,
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick,
                ) {
                    InterestsRoute(
                        viewModel = child.component,
                        onTopicClick = onTopicClick,
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeChildRoute(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        NiaTopAppBar(
            titleRes = titleResId,
            navigationIcon = NiaIcons.Search,
            navigationIconContentDescription = stringResource(
                id = SearchR.string.top_app_bar_navigation_icon_description,
            ),
            actionIcon = NiaIcons.Settings,
            actionIconContentDescription = stringResource(
                id = SettingsR.string.top_app_bar_action_icon_description,
            ),
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
            ),
            onActionClick = onSettingsClick,
            onNavigationClick = onSearchClick,
        )

        content()
    }
}

