package com.r3bl.stayawake

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log.d
import com.r3bl.stayawake.MyTileServiceSettings.Settings
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

/**
 * [Settings] are changed by the user interacting w/ [MainActivity]. These need to be propagated to all the other parts
 * of this application when that occurs.
 *
 * This class takes care of the following responsibilities:
 * 1. Handle exposing [Settings] for the app to all the Android components in the app in a thread-safe manner.
 * 2. Handle persisting any user initiated changes to the [Settings].
 */
object MyTileServiceSettings {

  // Settings related classes.

  /** Immutable Settings. */
  data class Settings(val autoStartEnabled: Boolean, val timeoutNotChargingSec: Long)

  /** Mutable [Settings] builder only used in DSL. */
  data class SettingsBuilder(var autoStartEnabled: Boolean? = null, var timeoutNotChargingSec: Long? = null)

  /**
   * DSL that allows [SettingsBuilder] to be changed, and only changed items are saved to persistence.
   */
  fun changeSettings(context: Context, lambda: SettingsBuilder.() -> Unit) {
    val settingsBuilder = SettingsBuilder()
    lambda(settingsBuilder)
    saveToPersistence(context, settingsBuilder)
    val settings: Settings = loadFromPersistence(context)
    fireSettingsChangedEvent(settings)
  }

  // EventBus stuff.

  /** Wrapper event for use with [EventBus]. */
  data class SettingsChangedEvent(val settings: Settings)

  private fun fireSettingsChangedEvent(settings: Settings) {
    EventBus.getDefault().post(SettingsChangedEvent(settings))
    d(MyTileService.TAG, "fireSettingsChangedEvent, $settings")
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

  private var DEFAULT_AUTO_START_ENABLED: Boolean = true
  private var DEFAULT_TIMEOUT_NOT_CHARGING_SEC: Long = TimeUnit.SECONDS.convert(10, TimeUnit.MINUTES)

  /** Returns a (immutable) [Settings] object containing the values that have been set, or the defaults. */
  fun loadFromPersistence(context: Context): Settings {
    val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val settings = Settings(
        prefs.getBoolean(context.getString(R.string.prefs_auto_start_enabled), DEFAULT_AUTO_START_ENABLED),
        prefs.getLong(context.getString(R.string.prefs_timeout_not_charging_sec), DEFAULT_TIMEOUT_NOT_CHARGING_SEC)
    )
    d(MyTileService.TAG, "loadSharedPreferences: $settings")
    return settings
  }

  /** This only saves the fields in [settingsBuilder] that have been set. */
  fun saveToPersistence(context: Context, settingsBuilder: SettingsBuilder) {
    val editPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit()
    // Only save fields that have been set.
    settingsBuilder.autoStartEnabled?.let {
      editPrefs.putBoolean(context.getString(R.string.prefs_auto_start_enabled), it)
    }
    settingsBuilder.timeoutNotChargingSec?.let {
      editPrefs.putLong(context.getString(R.string.prefs_timeout_not_charging_sec), it)
    }
    d(MyTileService.TAG, "saveSharedPreferences: $settingsBuilder")
    editPrefs.apply()
  }
}

/** Tailored class that uses [ThreadSafeCachedValueHolder] for [Settings] w/ a specific loader. */
class ThreadSafeSettingsHolder(private val context: Context) {
  private val myCachedValueHolder: ThreadSafeCachedValueHolder<Settings> =
      ThreadSafeCachedValueHolder(context) { MyTileServiceSettings.loadFromPersistence(context) }

  var value: Settings
    get() = myCachedValueHolder.cachedValue
    set(value) {
      myCachedValueHolder.cachedValue = value
    }

  override fun toString(): String = myCachedValueHolder.toString()
}
