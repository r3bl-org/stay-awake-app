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
import android.util.Log

/** Changes to Android broadcast receiver behaviors: http://tinyurl.com/y9rm5wzg */
class PowerConnectionReceiver : BroadcastReceiver() {
  private lateinit var myContext: Context
  override fun onReceive(context: Context, intent: Intent) {
    myContext = context
    val action = intent.action
    val message = "onReceive: PowerConnectionReceiver Action=$action"
    Log.d(MyTileService.TAG, message)
    when (action) {
      Intent.ACTION_POWER_CONNECTED    -> powerConnected()
      Intent.ACTION_POWER_DISCONNECTED -> powerDisconnected()
      else                             -> {
      }
    }
  }

  /** Stop service when power disconnected. */
  private fun powerDisconnected() {
    MyTileService.stopService(myContext)
    val message = "onReceive: PowerConnectionReceiver ACTION_POWER_DISCONNECTED ... Stop Service"
    //showToast(context, msg1);
    Log.d(MyTileService.TAG, message)
  }

  /** Start the [TileService] when power is connected. */
  private fun powerConnected() {
    MyTileService.startService(myContext)
    val message = "onReceive: PowerConnectionReceiver ACTION_POWER_CONNECTED ... Start Service"
    //showToast(context, msg1);
    Log.d(MyTileService.TAG, message)
  }

  fun register(context: Context) {
    // Register system broadcast receiver (to handle future power connection and disconnection events).
    context.registerReceiver(this, IntentFilter(Intent.ACTION_POWER_CONNECTED))
    context.registerReceiver(this, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
    Log.d(MyTileService.TAG, "registerReceiver: PowerConnectionReceiver")

  }

  fun unregister(context: Context) {
    context.unregisterReceiver(this)
  }
}