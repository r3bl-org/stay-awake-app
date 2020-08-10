package com.r3bl.stayawake

import android.content.Context
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * Thread safe holder for an object. The [loader] lambda is used to load the object if the [cachedValue] has not been
 * set.
 */
data class ThreadSafeCachedValueHolder<T>(private val context: Context, private val loader: () -> T) {
  private val mySettingsLock = ReentrantReadWriteLock()
  private var _cachedValue: T? = null

  /** The [cachedValue] can be accessed safely from background threads, and the main thread. */
  var cachedValue: T
    /** Update [_cachedValue] w/ the object that's passed. */
    set(value: T) = mySettingsLock.writeLock().withLock {
      _cachedValue = value
    }
    /** If [_cachedValue] isn't [set] then [loader] will be use to load the object. */
    get(): T = mySettingsLock.readLock().withLock {
      _cachedValue ?: loader().apply { _cachedValue = this }
    }

  override fun toString() = _cachedValue?.toString() ?: "Holder.value not set yet"
}