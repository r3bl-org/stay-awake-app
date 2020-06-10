/*
 * Copyright 2020 R3BL LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.r3bl.stayawake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log.d
import com.r3bl.stayawake.MyTileService.Companion.TAG
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/** Changes to Android broadcast receiver behaviors: http://tinyurl.com/y9rm5wzg */
class PowerConnectionReceiver(private val myContext: Context) : BroadcastReceiver() {
  /** Register system broadcast receiver (to handle future power connection and disconnection events). */
  init {
    myContext.registerReceiver(this, IntentFilter(Intent.ACTION_POWER_CONNECTED))
    myContext.registerReceiver(this, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
    MyTileServiceSettings.registerWithEventBus(this)
    d(TAG, "registerReceiver: PowerConnectionReceiver")
  }

  fun unregister() {
    MyTileServiceSettings.unregisterFromEventBus(this)
    myContext.unregisterReceiver(this)
  }

  private val mySettingsHolder = MyTileServiceSettings.Holder(myContext)

  /** Handle [SettingsChangedEvent] from [EventBus]. */
  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  fun onSettingsChangedEvent(event: MyTileServiceSettings.SettingsChangedEvent) = event.settings.apply {
    mySettingsHolder.value = this
    d(TAG, "PowerConnectionReceiver.onSettingsChangedEvent: ${mySettingsHolder}")
  }

  // Receiver functionality.
  override fun onReceive(context: Context, intent: Intent) {
    intent.action?.let { action ->
      val message = "onReceive: PowerConnectionReceiver Action=$action"
      d(TAG, message)
      when (action) {
        Intent.ACTION_POWER_CONNECTED    -> onPowerConnected()
        Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
      }
    }
  }

  private fun onPowerConnected() {
    if (mySettingsHolder.value.autoStartEnabled) {
      MyTileService.fireIntentWithStartService(myContext)
      val message = "onReceive: PowerConnectionReceiver ACTION_POWER_CONNECTED ... Start Service"
      // showToast(myContext, message)
      d(TAG, message)
    }
    else {
      val message = "onReceive: PowerConnectionReceiver ACTION_POWER_CONNECTED ... Do nothing, auto start disabled"
      // showToast(myContext, message)
      d(TAG, message)
    }
  }

  private fun onPowerDisconnected() {
    if (mySettingsHolder.value.autoStartEnabled) {
      MyTileService.fireIntentWithStopService(myContext)
      val message = "onReceive: PowerConnectionReceiver ACTION_POWER_DISCONNECTED ... Stop Service"
      // showToast(myContext, message)
      d(TAG, message)
    }
    else {
      val message = "onReceive: PowerConnectionReceiver ACTION_POWER_DISCONNECTED ... Do nothing, auto start disabled"
      // showToast(myContext, message)
      d(TAG, message)
    }
  }
}