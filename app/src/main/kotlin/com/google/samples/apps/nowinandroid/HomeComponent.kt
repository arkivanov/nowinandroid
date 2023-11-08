package com.google.samples.apps.nowinandroid

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.google.samples.apps.nowinandroid.feature.bookmarks.BookmarksViewModel
import com.google.samples.apps.nowinandroid.feature.foryou.ForYouViewModel
import com.google.samples.apps.nowinandroid.feature.foryou.navigation.ForYouArgs
import com.google.samples.apps.nowinandroid.feature.interests.InterestsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable

class HomeComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted deeplink: Deeplink?,
    private val forYouFactory: ForYouViewModel.Factory,
    private val bookmarksFactory: BookmarksViewModel.Factory,
    private val interestsFactory: InterestsViewModel.Factory,
) : ComponentContext by componentContext {

    private val nav = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = nav,
            serializer = Config.serializer(),
            initialConfiguration = getInitialConfig(deeplink),
            childFactory = ::child,
        )

    private fun child(config: Config, ctx: ComponentContext): Child =
        when (config) {
            is Config.ForYou -> Child.ForYou(forYouFactory(ctx, ForYouArgs()))
            is Config.Bookmarks -> Child.Bookmarks(bookmarksFactory(ctx))
            is Config.Interests -> Child.Interests(interestsFactory(ctx))
        }

    fun navigateToForYou() {
        nav.bringToFront(Config.ForYou)
    }

    fun navigateToBookmarks() {
        nav.bringToFront(Config.Bookmarks)
    }

    fun navigateToInterests() {
        nav.bringToFront(Config.Interests)
    }

    private fun getInitialConfig(deeplink: Deeplink?): Config =
        when (deeplink) {
            is Deeplink.Interests -> Config.Interests
            null -> Config.ForYou
        }

    sealed interface Child {
        class ForYou(val component: ForYouViewModel) : Child
        class Bookmarks(val component: BookmarksViewModel) : Child
        class Interests(val component: InterestsViewModel) : Child
    }

    sealed interface Deeplink {
        data object Interests : Deeplink
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object ForYou : Config

        @Serializable
        data object Bookmarks : Config

        @Serializable
        data object Interests : Config
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, deeplink: Deeplink?): HomeComponent
    }
}