package com.ironlogic.veyra.data.video

import kotlinx.coroutines.CancellationException

/** Keeps optional metadata failures non-fatal without hiding revoked SAF permissions. */
object VideoFolderMetadataPolicy {
    fun readDuration(read: () -> Long): Long = try {
        read().coerceAtLeast(0L)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (security: SecurityException) {
        throw security
    } catch (_: Exception) {
        0L
    }
}
