package com.r3bl.stayawake

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import java.util.concurrent.TimeUnit

object MyTileServiceSettings {
  var autoStartEnabled: Boolean = true
  var timeoutNotChargingSec: Long = TimeUnit.SECONDS.convert(10,
                                                             TimeUnit.MINUTES)

  fun loadSharedPreferences(context: Context) = with(context) {
    with(PreferenceManager.getDefaultSharedPreferences(this)) {
      autoStartEnabled = getBoolean(getString(R.string.prefs_auto_start_enabled), autoStartEnabled)
      timeoutNotChargingSec = getLong(getString(R.string.prefs_timeout_not_charging_sec), timeoutNotChargingSec)
      Log.d(MyTileService.TAG,
            "loadSharedPreferences: $debugString")
    }
  }

  private fun saveSharedPreferences(context: Context) = with(context) {
    with(PreferenceManager.getDefaultSharedPreferences(this)) {
      with(edit()) {
        putBoolean(getString(R.string.prefs_auto_start_enabled), autoStartEnabled)
        putLong(getString(R.string.prefs_timeout_not_charging_sec), timeoutNotChargingSec)
        commit()
      }
    }
  }

  fun saveSharedPreferencesAfterRunningLambda(context: Context, lambda: MyTileServiceSettings.() -> Unit) {
    lambda(this)
    saveSharedPreferences(context)
    Log.d(MyTileService.TAG,
          "saveSharedPreferences, $debugString")
  }

  private val debugString: String
    get() = "autoStartEnabled = $autoStartEnabled, timeoutNotChargingSec = $timeoutNotChargingSec"

}