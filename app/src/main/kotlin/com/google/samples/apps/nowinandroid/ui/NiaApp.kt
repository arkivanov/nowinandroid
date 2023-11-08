/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.google.samples.apps.nowinandroid.HomeComponent
import com.google.samples.apps.nowinandroid.HomeComponent.Child
import com.google.samples.apps.nowinandroid.HomeComponent.Child.Bookmarks
import com.google.samples.apps.nowinandroid.HomeComponent.Child.ForYou
import com.google.samples.apps.nowinandroid.HomeComponent.Child.Interests
import com.google.samples.apps.nowinandroid.HomeRoute
import com.google.samples.apps.nowinandroid.NiaScaffold
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.RootComponent
import com.google.samples.apps.nowinandroid.RootComponent.Child.Home
import com.google.samples.apps.nowinandroid.RootComponent.Child.Search
import com.google.samples.apps.nowinandroid.RootComponent.Child.Topic
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationBar
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationBarItem
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationRail
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationRailItem
import com.google.samples.apps.nowinandroid.feature.search.SearchRoute
import com.google.samples.apps.nowinandroid.feature.settings.SettingsDialog
import com.google.samples.apps.nowinandroid.feature.topic.TopicRoute
import com.google.samples.apps.nowinandroid.navigation.TopLevelDestination

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NiaApp(
    component: RootComponent,
    windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    userNewsResourceRepository: UserNewsResourceRepository,
    appState: NiaAppState = rememberNiaAppState(
        networkMonitor = networkMonitor,
        userNewsResourceRepository = userNewsResourceRepository,
    ),
) {
    var showSettingsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    NiaBackground(modifier = Modifier.fillMaxSize()) {
        val snackbarHostState = remember { SnackbarHostState() }

        val isOffline by appState.isOffline.collectAsStateWithLifecycle()

        // If user is not connected to the internet show a snack bar to inform them.
        val notConnectedMessage = stringResource(R.string.not_connected)
        LaunchedEffect(isOffline) {
            if (isOffline) {
                snackbarHostState.showSnackbar(
                    message = notConnectedMessage,
                    duration = Indefinite,
                )
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = component.settings,
                onDismiss = { showSettingsDialog = false },
            )
        }

        Children(
            component = component,
            snackbarHostState = snackbarHostState,
            windowSizeClass = windowSizeClass,
            appState = appState,
            onSettingsClick = { showSettingsDialog = true },
            modifier = Modifier.semantics { testTagsAsResourceId = true },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Children(
    component: RootComponent,
    snackbarHostState: SnackbarHostState,
    windowSizeClass: WindowSizeClass,
    appState: NiaAppState,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NiaScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Children(
            stack = component.stack,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
            animation = predictiveBackAnimation(
                backHandler = component.backHandler,
                animation = stackAnimation { child, _, direction ->
                    when {
                        child.instance is Search && direction.isFront-> slide(orientation = Vertical) + fade()
                        direction.isFront -> slide() + fade()
                        else -> scale(frontFactor = 1F, backFactor = 0.7F) + fade()
                    }
                },
                onBack = component::navigateBack,
            ),
        ) {
            when (val child = it.instance) {
                is Home ->
                    HomeRoute(
                        component = child.component,
                        windowSizeClass = windowSizeClass,
                        appState = appState,
                        onSettingsClick = onSettingsClick,
                        onSearchClick = component::navigateToSearch,
                        onTopicClick = component::navigateToTopic,
                        onShowSnackbar = { message, action ->
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = action,
                                duration = Short,
                            ) == ActionPerformed
                        },
                    )

                is Topic ->
                    NiaBackground {
                        TopicRoute(
                            viewModel = child.component,
                            onBackClick = component::navigateBack,
                            onTopicClick = component::navigateToTopic,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                is Search ->
                    NiaBackground {
                        SearchRoute(
                            searchViewModel = child.component,
                            modifier = Modifier.fillMaxSize(),
                            onBackClick = component::navigateBack,
                            onInterestsClick = component::navigateToInterests,
                            onTopicClick = component::navigateToTopic,
                        )
                    }
            }
        }
    }
}

@Composable
fun NiaNavRail(
    component: HomeComponent,
    destinationsWithUnreadResources: Set<TopLevelDestination>,
    modifier: Modifier = Modifier,
) {
    val stack by component.stack.subscribeAsState()
    val child by remember { derivedStateOf { stack.active.instance } }

    NiaNavigationRail(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == child.destination
            val hasUnread = destinationsWithUnreadResources.contains(destination)
            NiaNavigationRailItem(
                selected = selected,
                onClick = { component.navigateTo(destination) },
                icon = {
                    Icon(
                        imageVector = destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.iconTextId)) },
                modifier = if (hasUnread) Modifier.notificationDot() else Modifier,
            )
        }
    }
}

@Composable
fun NiaBottomBar(
    component: HomeComponent,
    destinationsWithUnreadResources: Set<TopLevelDestination>,
    modifier: Modifier = Modifier,
) {
    val stack by component.stack.subscribeAsState()
    val child by remember { derivedStateOf { stack.active.instance } }

    NiaNavigationBar(
        modifier = modifier,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val hasUnread = destinationsWithUnreadResources.contains(destination)
            val selected = destination == child.destination
            NiaNavigationBarItem(
                selected = selected,
                onClick = { component.navigateTo(destination) },
                icon = {
                    Icon(
                        imageVector = destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.iconTextId)) },
                modifier = if (hasUnread) Modifier.notificationDot() else Modifier,
            )
        }
    }
}

private fun HomeComponent.navigateTo(destination: TopLevelDestination) {
    when (destination) {
        TopLevelDestination.FOR_YOU -> navigateToForYou()
        TopLevelDestination.BOOKMARKS -> navigateToBookmarks()
        TopLevelDestination.INTERESTS -> navigateToInterests()
    }
}

private val Child.destination: TopLevelDestination
    get() =
        when (this) {
            is ForYou -> TopLevelDestination.FOR_YOU
            is Bookmarks -> TopLevelDestination.BOOKMARKS
            is Interests -> TopLevelDestination.INTERESTS
        }

private fun Modifier.notificationDot(): Modifier =
    composed {
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        drawWithContent {
            drawContent()
            drawCircle(
                tertiaryColor,
                radius = 5.dp.toPx(),
                // This is based on the dimensions of the NavigationBar's "indicator pill";
                // however, its parameters are private, so we must depend on them implicitly
                // (NavigationBarTokens.ActiveIndicatorWidth = 64.dp)
                center = center + Offset(
                    64.dp.toPx() * .45f,
                    32.dp.toPx() * -.45f - 6.dp.toPx(),
                ),
            )
        }
    }
