package com.google.samples.apps.nowinandroid.core.decompose.utils

import com.arkivanov.essenty.statekeeper.StateKeeperOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer

fun <T : Any> StateKeeperOwner.saveableStateFlow(
    key: String,
    serializer: KSerializer<T>,
    initialValue: () -> T,
): MutableStateFlow<T> {
    val flow =
        MutableStateFlow(
            value = stateKeeper.consume(key = key, strategy = serializer) ?: initialValue(),
        )

    stateKeeper.register(key = key, strategy = serializer) { flow.value }

    return flow
}
