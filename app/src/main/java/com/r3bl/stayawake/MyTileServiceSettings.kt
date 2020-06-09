package com.r3bl.stayawake

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import java.util.concurrent.TimeUnit

object MyTileServiceSettings {
  private var DEFAULT_AUTO_START_ENABLED: Boolean = true
  private var DEFAULT_TIMEOUT_NOT_CHARGING_SEC: Long = TimeUnit.SECONDS.convert(10, TimeUnit.MINUTES)

  data class SettingsImmutable(val autoStartEnabled: Boolean, val timeoutNotChargingSec: Long)

  data class SettingsMutable(var autoStartEnabled: Boolean? = null, var timeoutNotChargingSec: Long? = null)

  fun loadSharedPreferences(context: Context): SettingsImmutable = with(context) {
    with(PreferenceManager.getDefaultSharedPreferences(this)) {
      return SettingsImmutable(
          getBoolean(getString(R.string.prefs_auto_start_enabled), DEFAULT_AUTO_START_ENABLED),
          getLong(getString(R.string.prefs_timeout_not_charging_sec), DEFAULT_TIMEOUT_NOT_CHARGING_SEC)
      ).apply {
        Log.d(MyTileService.TAG, "loadSharedPreferences: $this")
      }
    }
  }

  fun saveSharedPreferencesAfterRunningLambda(context: Context, lambda: SettingsMutable.() -> Unit) {
    val settings = SettingsMutable()
    lambda(settings)
    saveSharedPreferences(context, settings)
  }

  private fun saveSharedPreferences(context: Context, settingsMutable: SettingsMutable) = with(context) {
    with(PreferenceManager.getDefaultSharedPreferences(this)) {
      with(edit()) {
        // Only save fields that have been set.
        settingsMutable.autoStartEnabled?.apply {
          putBoolean(getString(R.string.prefs_auto_start_enabled), this)
        }
        settingsMutable.timeoutNotChargingSec?.apply {
          putLong(getString(R.string.prefs_timeout_not_charging_sec), this)
        }
        Log.d(MyTileService.TAG, "saveSharedPreferences, $settingsMutable")
        commit()
      }
    }
  }
}