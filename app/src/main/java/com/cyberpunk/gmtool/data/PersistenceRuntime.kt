package com.cyberpunk.gmtool.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Process-wide persistence coordination.
 *
 * - All long-lived JSON stores and whole-game snapshots share one re-entrant lock.
 * - restoreEpoch changes only after a successful whole-game restore. UI/store writers capture
 *   the epoch they were created under, so stale Compose state cannot overwrite restored files.
 * - reloadGeneration is observable by Compose; a restore rebuilds file-backed screens/tools.
 */
object PersistenceRuntime {
    private val ioLock = ReentrantLock(true)
    private val epoch = AtomicLong(0L)
    private val refreshCounter = AtomicLong(0L)
    private val _reloadGeneration = MutableStateFlow(0L)

    val reloadGeneration: StateFlow<Long> = _reloadGeneration.asStateFlow()
    fun currentEpoch(): Long = epoch.get()
    fun isCurrent(expectedEpoch: Long): Boolean = epoch.get() == expectedEpoch

    fun <T> locked(block: () -> T): T = ioLock.withLock(block)

    fun publishSuccessfulRestore() {
        epoch.incrementAndGet()
        _reloadGeneration.value = refreshCounter.incrementAndGet()
    }

    /** Refresh file-backed UI after a cross-tool write without invalidating the current restore epoch. */
    fun publishExternalDataChange() {
        _reloadGeneration.value = refreshCounter.incrementAndGet()
    }
}
