package com.r3bl.stayawake

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.util.Log.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * 1. Handle exposing [Settings] for the app to all the Android components in the app in a thread-safe manner.
 * 2. Handle persisting any user initiated changes to the [Settings].
 */
object MyTileServiceSettings {
  // Publicly exposed things.
  /** Immutable Settings. */
  data class Settings(val autoStartEnabled: Boolean, val timeoutNotChargingSec: Long)

  /** Mutable Settings only used in DSL. */
  data class MutableSettings(var autoStartEnabled: Boolean? = null, var timeoutNotChargingSec: Long? = null)

  /**
   * DSL that allows [MutableSettings] to be changed, and only changed items are saved to persistence.
   */
  fun changeSettings(context: Context, lambda: MutableSettings.() -> Unit) {
    val settings = MutableSettings()
    lambda(settings)
    saveToPersistence(context, settings)
    fireSettingsChangedEvent(settings)
  }

  /** Wrapper event for use with [EventBus]. */
  data class SettingsChangedEvent(val settings: Settings)

  fun registerWithEventBus(caller: Any) {
    EventBus.getDefault().register(caller)
  }

  fun unregisterFromEventBus(caller: Any) {
    EventBus.getDefault().unregister(caller)
  }

  /** Wrapper class for a thread-safe [Settings] object. */
  data class Holder(private val myContext: Context) {
    private val mySettingsLock = ReentrantReadWriteLock()
    private var _mySettings: Settings? = null

    var value: Settings
      set(value) = mySettingsLock.writeLock().withLock {
        _mySettings = value
      }
      get() = mySettingsLock.readLock().withLock {
        _mySettings ?: loadFromPersistence(myContext).apply { _mySettings = this }
      }

    override fun toString() = _mySettings?.toString() ?: "Holder.value not set yet"
  }

  // Hidden internal details.
  private var DEFAULT_AUTO_START_ENABLED: Boolean = true
  private var DEFAULT_TIMEOUT_NOT_CHARGING_SEC: Long = TimeUnit.SECONDS.convert(10, TimeUnit.MINUTES)

  private fun loadFromPersistence(context: Context): Settings = with(context) {
    with(PreferenceManager.getDefaultSharedPreferences(this)) {
      return Settings(
          getBoolean(getString(R.string.prefs_auto_start_enabled), DEFAULT_AUTO_START_ENABLED),
          getLong(getString(R.string.prefs_timeout_not_charging_sec), DEFAULT_TIMEOUT_NOT_CHARGING_SEC)
      ).apply {
        d(MyTileService.TAG, "loadSharedPreferences: $this")
      }
    }
  }

  private fun saveToPersistence(context: Context, mutableSettings: MutableSettings) = with(context) {
    with(PreferenceManager.getDefaultSharedPreferences(this)) {
      with(edit()) {
        // Only save fields that have been set.
        mutableSettings.autoStartEnabled?.apply {
          putBoolean(getString(R.string.prefs_auto_start_enabled), this)
        }
        mutableSettings.timeoutNotChargingSec?.apply {
          putLong(getString(R.string.prefs_timeout_not_charging_sec), this)
        }
        d(MyTileService.TAG, "saveSharedPreferences, $mutableSettings")
        apply()
      }
    }
  }

  private fun fireSettingsChangedEvent(settings: MutableSettings) {
    with(settings) {
      EventBus.getDefault().post(
          SettingsChangedEvent(Settings(autoStartEnabled ?: DEFAULT_AUTO_START_ENABLED,
                                        timeoutNotChargingSec ?: DEFAULT_TIMEOUT_NOT_CHARGING_SEC)
          ))
      d(MyTileService.TAG, "fireSettingsChangedEvent, $settings")
    }
  }
}