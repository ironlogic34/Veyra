package com.ironlogic.veyra.data.video

import kotlinx.coroutines.CancellationException

/** Keeps optional thumbnail failures non-fatal without swallowing coroutine cancellation. */
object VideoThumbnailLoadPolicy {
    inline fun <T> runCatchingWithoutCancellation(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
