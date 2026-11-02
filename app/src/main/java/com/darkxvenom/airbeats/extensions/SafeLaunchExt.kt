package com.darkxvenom.airbeats.extensions

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launches a coroutine that safely catches unexpected exceptions without
 * swallowing [CancellationException], preserving structured concurrency.
 */
fun CoroutineScope.launchCatching(
    context: CoroutineContext = EmptyCoroutineContext,
    tag: String = "AirBeatsCoroutine",
    onError: ((Throwable) -> Unit)? = null,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        runCatching { Log.e(tag, "Unhandled exception in coroutine", t) }
        onError?.invoke(t)
    }
}

/**
 * Executes [block] and encapsulates its result, rethrowing [CancellationException]
 * so suspend callers never accidentally suppress coroutine cancellation.
 */
inline fun <R> runCatchingNonCancellation(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

/**
 * Emits only the first item during each [windowDurationMs] interval,
 * preventing rapid duplicate UI clicks or repeated media triggers.
 */
fun <T> Flow<T>.throttleFirst(windowDurationMs: Long): Flow<T> = flow {
    require(windowDurationMs >= 0L) { "windowDurationMs must be non-negative" }
    var lastEmissionTimeMs = 0L
    var hasEmitted = false
    collect { value ->
        val nowMs = System.nanoTime() / 1_000_000L
        if (!hasEmitted || nowMs - lastEmissionTimeMs >= windowDurationMs) {
            hasEmitted = true
            lastEmissionTimeMs = nowMs
            emit(value)
        }
    }
}
