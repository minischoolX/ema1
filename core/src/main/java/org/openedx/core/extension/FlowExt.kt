package org.openedx.core.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

suspend inline fun <T> Flow<T?>.collectLatestNotNull(
    crossinline action: suspend (value: T) -> Unit
) {
    this.filterNotNull().collectLatest { action(it) }
}
