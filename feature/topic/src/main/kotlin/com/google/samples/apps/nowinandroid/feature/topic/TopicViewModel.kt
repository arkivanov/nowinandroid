/*
 * Copyright 2021 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.feature.topic

import com.arkivanov.decompose.ComponentContext
import com.google.samples.apps.nowinandroid.core.data.repository.NewsResourceQuery
import com.google.samples.apps.nowinandroid.core.data.repository.TopicsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.decompose.utils.coroutineScope
import com.google.samples.apps.nowinandroid.core.model.data.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.result.Result
import com.google.samples.apps.nowinandroid.core.result.asResult
import com.google.samples.apps.nowinandroid.feature.topic.navigation.TopicArgs
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicViewModel @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted private val topicArgs: TopicArgs,
    private val userDataRepository: UserDataRepository,
    topicsRepository: TopicsRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
) : ComponentContext by componentContext {

    private val viewModelScope = coroutineScope()

    val topicId = topicArgs.topicId

    val topicUiState: StateFlow<TopicUiState> = topicUiState(
        topicId = topicArgs.topicId,
        userDataRepository = userDataRepository,
        topicsRepository = topicsRepository,
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TopicUiState.Loading,
        )

    val newUiState: StateFlow<NewsUiState> = newsUiState(
        topicId = topicArgs.topicId,
        userDataRepository = userDataRepository,
        userNewsResourceRepository = userNewsResourceRepository,
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NewsUiState.Loading,
        )

    fun followTopicToggle(followed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(topicArgs.topicId, followed)
        }
    }

    fun bookmarkNews(newsResourceId: String, bookmarked: Boolean) {
        viewModelScope.launch {
            userDataRepository.updateNewsResourceBookmark(newsResourceId, bookmarked)
        }
    }

    fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, args: TopicArgs): TopicViewModel
    }
}

private fun topicUiState(
    topicId: String,
    userDataRepository: UserDataRepository,
    topicsRepository: TopicsRepository,
): Flow<TopicUiState> {
    // Observe the followed topics, as they could change over time.
    val followedTopicIds: Flow<Set<String>> =
        userDataRepository.userData
            .map { it.followedTopics }

    // Observe topic information
    val topicStream: Flow<Topic> = topicsRepository.getTopic(
        id = topicId,
    )

    return combine(
        followedTopicIds,
        topicStream,
        ::Pair,
    )
        .asResult()
        .map { followedTopicToTopicResult ->
            when (followedTopicToTopicResult) {
                is Result.Success -> {
                    val (followedTopics, topic) = followedTopicToTopicResult.data
                    val followed = followedTopics.contains(topicId)
                    TopicUiState.Success(
                        followableTopic = FollowableTopic(
                            topic = topic,
                            isFollowed = followed,
                        ),
                    )
                }

                is Result.Loading -> {
                    TopicUiState.Loading
                }

                is Result.Error -> {
                    TopicUiState.Error
                }
            }
        }
}

private fun newsUiState(
    topicId: String,
    userNewsResourceRepository: UserNewsResourceRepository,
    userDataRepository: UserDataRepository,
): Flow<NewsUiState> {
    // Observe news
    val newsStream: Flow<List<UserNewsResource>> = userNewsResourceRepository.observeAll(
        NewsResourceQuery(filterTopicIds = setOf(element = topicId)),
    )

    // Observe bookmarks
    val bookmark: Flow<Set<String>> = userDataRepository.userData
        .map { it.bookmarkedNewsResources }

    return combine(
        newsStream,
        bookmark,
        ::Pair,
    )
        .asResult()
        .map { newsToBookmarksResult ->
            when (newsToBookmarksResult) {
                is Result.Success -> {
                    val news = newsToBookmarksResult.data.first
                    NewsUiState.Success(news)
                }

                is Result.Loading -> {
                    NewsUiState.Loading
                }

                is Result.Error -> {
                    NewsUiState.Error
                }
            }
        }
}

sealed interface TopicUiState {
    data class Success(val followableTopic: FollowableTopic) : TopicUiState
    data object Error : TopicUiState
    data object Loading : TopicUiState
}

sealed interface NewsUiState {
    data class Success(val news: List<UserNewsResource>) : NewsUiState
    data object Error : NewsUiState
    data object Loading : NewsUiState
}
