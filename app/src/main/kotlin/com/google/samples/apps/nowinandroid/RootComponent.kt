package com.google.samples.apps.nowinandroid

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.google.samples.apps.nowinandroid.feature.search.SearchViewModel
import com.google.samples.apps.nowinandroid.feature.settings.SettingsViewModel
import com.google.samples.apps.nowinandroid.feature.topic.TopicViewModel
import com.google.samples.apps.nowinandroid.feature.topic.navigation.TopicArgs
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable

class RootComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    private val homeFactory: HomeComponent.Factory,
    private val topicFactory: TopicViewModel.Factory,
    private val searchFactory: SearchViewModel.Factory,
    settingsFactory: SettingsViewModel.Factory,
) : ComponentContext by componentContext, BackHandlerOwner {

    val settings: SettingsViewModel = settingsFactory(childContext(key = "Settings"))

    private val nav = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = nav,
            serializer = Config.serializer(),
            initialConfiguration = Config.Home(),
            childFactory = ::child,
        )

    private fun child(config: Config, ctx: ComponentContext): Child =
        when (config) {
            is Config.Home -> Child.Home(homeFactory(ctx, config.deepLink))
            is Config.Topic -> Child.Topic(topicFactory(ctx, config.args))
            is Config.Search -> Child.Search(searchFactory(ctx))
        }

    fun navigateToTopic(topicId: String) {
        nav.pushNew(Config.Topic(args = TopicArgs(topicId = topicId)))
    }

    fun navigateToSearch() {
        nav.pushNew(Config.Search)
    }

    fun navigateToInterests() {
        nav.replaceAll(Config.Home(deepLink = HomeComponent.Deeplink.Interests))
    }

    fun navigateBack() {
        nav.pop()
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): RootComponent
    }

    sealed interface Child {
        class Home(val component: HomeComponent) : Child
        class Topic(val component: TopicViewModel) : Child
        class Search(val component: SearchViewModel) : Child
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class Home(val deepLink: HomeComponent.Deeplink? = null) : Config

        @Serializable
        data class Topic(val args: TopicArgs) : Config

        @Serializable
        data object Search : Config
    }
}