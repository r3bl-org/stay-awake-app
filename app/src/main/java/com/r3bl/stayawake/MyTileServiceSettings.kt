package com.r3bl.stayawake

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log.d
import com.r3bl.stayawake.MyTileServiceSettings.Settings
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * [Settings] are changed by the user interacting w/ [MainActivity]. These need to be propagated to all the other parts
 * of this application when that occurs.
 *
 * This class takes care of the following responsibilities:
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

  // EventBus stuff.

  /** Wrapper event for use with [EventBus]. */
  data class SettingsChangedEvent(val settings: Settings)

  private fun fireSettingsChangedEvent(settings: MutableSettings) {
    with(settings) {
      EventBus.getDefault().post(
          SettingsChangedEvent(Settings(autoStartEnabled ?: DEFAULT_AUTO_START_ENABLED,
                                        timeoutNotChargingSec ?: DEFAULT_TIMEOUT_NOT_CHARGING_SEC)
          ))
      d(MyTileService.TAG, "fireSettingsChangedEvent, $settings")
    }
  }

  /**
   * Register the [caller] w/ the event bus. Any changes to the [Settings] will fire an [SettingsChangedEvent] to the
   * [caller].
   */
  fun registerWithEventBus(caller: Any) {
    EventBus.getDefault().register(caller)
  }

  /** Unregister the [caller] w/ the event bus. */
  fun unregisterFromEventBus(caller: Any) {
    EventBus.getDefault().unregister(caller)
  }

  // Thread safe wrapper for [Settings].

  data class ThreadSafeSettingsWrapper(private val myContext: Context) {
    private val mySettingsLock = ReentrantReadWriteLock()
    private var _mySettings: Settings? = null

    /** The [value] can be accessed safely from background threads, and the main thread. */
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

  private fun loadFromPersistence(context: Context): Settings {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val immutableSettings = Settings(
        sharedPreferences.getBoolean(context.getString(R.string.prefs_auto_start_enabled),
                                     DEFAULT_AUTO_START_ENABLED),
        sharedPreferences.getLong(context.getString(R.string.prefs_timeout_not_charging_sec),
                                  DEFAULT_TIMEOUT_NOT_CHARGING_SEC)
    )
    d(MyTileService.TAG, "loadSharedPreferences: $immutableSettings")
    return immutableSettings
  }

  private fun saveToPersistence(context: Context, mutableSettings: MutableSettings) {
    val sharedPreferencesEdit = PreferenceManager.getDefaultSharedPreferences(context).edit()
    // Only save fields that have been set.
    mutableSettings.autoStartEnabled?.let {
      sharedPreferencesEdit.putBoolean(context.getString(R.string.prefs_auto_start_enabled), it)
    }
    mutableSettings.timeoutNotChargingSec?.let {
      sharedPreferencesEdit.putLong(context.getString(R.string.prefs_timeout_not_charging_sec), it)
    }
    d(MyTileService.TAG, "saveSharedPreferences, $mutableSettings")
    sharedPreferencesEdit.apply()
  }
}