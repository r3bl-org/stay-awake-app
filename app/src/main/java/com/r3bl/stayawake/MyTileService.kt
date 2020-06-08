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

import android.annotation.TargetApi
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.util.Log.d
import androidx.annotation.MainThread
import com.r3bl.stayawake.MyTileServiceSettings.loadSharedPreferences
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * This is a bound and started service. TileService is a bound service, and it automatically binds to the Settings Tile.
 * Since this service also holds a WakeLock, this part of it happens in the started service, which also displays a
 * persistent notification, and takes care of starting itself.
 *
 * If the user has added this quick tile to their notification drawer, then [onCreate] will be called when the device
 * boots. This in turn attaches the [PowerConnection] which allows Awake to start automatically when the device
 * is charged. If they don't add the quick tile, then this does not happen.
 */
class MyTileService : TileService() {
  // Data.
  private var myTimeRunning_sec: Long = 0
  private var myWakeLock: WakeLock? = null
  private var myExecutor: ScheduledExecutorService? = null
  private var myServiceIsStarted = false
  private var myIconEyeOpen: Icon? = null
  private var myIconEyeClosed: Icon? = null
  private var myHandler: Handler? = null
  private var myReceiver: PowerConnectionReceiver? = null

  // General service code.
  override fun onCreate() {
    super.onCreate()
    myHandler = Handler()
    myIconEyeOpen = Icon.createWithResource(this, R.drawable.ic_stat_visibility)
    myIconEyeClosed = Icon.createWithResource(this, R.drawable.ic_stat_visibility_off)
    loadSharedPreferences(this)
    myReceiver = PowerConnectionReceiver(this)
    if (isCharging(this)) commandStart()
    d(TAG, "onCreate: ")
  }

  override fun onDestroy() {
    super.onDestroy()
    d(TAG, "onDestroy: ")
    myExecutor?.apply {
      shutdownNow()
      d(TAG, "onDestroy: stopping executor")
    }
    updateTile()
    myReceiver?.apply {
      unregister()
      d(TAG, "unregisterReceiver: PowerConnectionReceiver")
    }
  }

  // Bound Service code & TileService code.
  override fun onTileAdded() {
    super.onTileAdded()
    d(TAG, "onTileAdded: ")
    updateTile()
  }

  override fun onTileRemoved() {
    super.onTileRemoved()
    d(TAG, "onTileRemoved: ")
  }

  override fun onStartListening() {
    super.onStartListening()
    d(TAG, "onStartListening: ")
    updateTile()
  }

  override fun onStopListening() {
    super.onStopListening()
    d(TAG, "onStopListening: ")
    updateTile()
  }

  override fun onClick() {
    super.onClick()
    if (myServiceIsStarted) {
      d(TAG, "onClick: calling commandStop()")
      commandStop()
    }
    else {
      d(TAG, "onClick: calling commandStart()")
      commandStart()
    }
    updateTile()
  }

  // Started service code.
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    d(TAG, getDebugIntentString(intent, startId))
    routeIntentToCommand(intent)
    return Service.START_NOT_STICKY
  }

  private fun getDebugIntentString(intent: Intent, startId: Int): String {
    val containsCommand = IntentHelper.containsCommand(intent)
    return String.format(
        "onStartCommand: Service in [%s] state. commandId: [%d]. startId: [%d]",
        if (myServiceIsStarted) "STARTED" else "NOT STARTED",
        if (containsCommand) IntentHelper.getCommand(intent) else "N/A",
        startId)
  }

  private fun routeIntentToCommand(intent: Intent?) {
    intent?.apply {
      // Process command.
      if (IntentHelper.containsCommand(this)) {
        processCommand(IntentHelper.getCommand(this))
      }
      // Process message.
      if (IntentHelper.containsMessage(this)) {
        processMessage(IntentHelper.getMessage(this))
      }
    }
  }

  private fun processMessage(message: String) {
    try {
      // Do nothing.
      d(TAG,
        String.format("doMessage: message from client: '%s'", message))
    }
    catch (e: Exception) {
      Log.e(TAG, "processMessage: exception", e)
    }
  }

  private fun processCommand(command: Int) {
    try {
      when (command) {
        CommandId.START -> commandStart()
        CommandId.STOP  -> commandStop()
      }
    }
    catch (e: Exception) {
      Log.e(TAG, "processCommand: exception", e)
    }
  }

  /**
   * This method can be called directly, or by firing an explicit Intent with [CommandId.STOP].
   */
  private fun commandStop() {
    if (!myServiceIsStarted) return

    try {
      releaseWakeLock()
      stopForeground(true)
      stopSelf()
      myExecutor?.apply {
        shutdown()
        myExecutor = null
      }
      updateTile()
    }
    finally {
      myServiceIsStarted = false
    }
  }

  /**
   * This can be called via an explicit intent to start this serivce, which calls [.onStartCommand] or it can be called directly, which is what happens in [.onClick] by this bound service.
   *
   *
   *
   * This is why the service needs to [.moveToStartedState] if it's not already in a started state. More
   * details can be found in the method documentation itself.
   */
  private fun commandStart() {
    if (myServiceIsStarted) return

    try {
      moveToStartedState()
      moveToForegroundAndShowNotification()
      acquireWakeLock()
      startExecutor()
    }
    finally {
      myServiceIsStarted = true
    }
  }

  private fun startExecutor() {
    if (myExecutor == null) {
      myTimeRunning_sec = 0
      myExecutor = Executors.newSingleThreadScheduledExecutor().apply {
        scheduleWithFixedDelay({ myHandler?.post { recurringTask() } },
                               DELAY_INITIAL.toLong(),
                               DELAY_RECURRING.toLong(),
                               DELAY_UNIT)

      }
      d(TAG, "commandStart: starting executor")
    }
    else {
      d(TAG, "commandStart: executor not started")
    }
  }

  private fun moveToForegroundAndShowNotification() {
    when {
      isPreAndroidO -> HandleNotifications.PreO.createNotification(this)
      else          -> HandleNotifications.O.createNotification(this)
    }
  }

  /**
   * If a call is made to [.commandStart] without firing an explicit Intent to put this service in a started state
   * (which happens in [.onClick]), then fire the explicit intent with [CommandId.START] which actually ends
   * up calling [.commandStart] again and this time, does the work of creating the executor.
   *
   * Next, you would move this service into the foreground, which you can't do unless this service is in a started
   * state.
   */
  @TargetApi(Build.VERSION_CODES.O)
  private fun moveToStartedState() {
    if (isPreAndroidO) {
      d(TAG, "moveToStartedState: Running on Android N or lower - startService(intent)")
      startService(MyIntentBuilder.getExplicitIntentToStartService(this))
    }
    else {
      d(TAG, "moveToStartedState: Running on Android O - startForegroundService(intent)")
      startForegroundService(MyIntentBuilder.getExplicitIntentToStartService(this))
    }
  }

  private fun acquireWakeLock() {
    if (myWakeLock == null) {
      val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
      myWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG).apply { acquire() }
      d(TAG, "acquireWakeLock: ")
    }
  }

  private fun releaseWakeLock() {
    if (myWakeLock != null) {
      myWakeLock?.release()
      myWakeLock = null
      d(TAG, "releaseWakeLock: ")
    }
  }

  @MainThread
  private fun recurringTask() {
    if (isCharging(this)) {
      // Reset the countdown timer.
      myTimeRunning_sec = 0
    }
    else {
      // Run down the countdown timer.
      myTimeRunning_sec++
      if (myTimeRunning_sec >= MyTileServiceSettings.timeoutNotChargingSec) {
        // Timer has run out.
        if (isCharging(this)) {
          d(TAG, "recurringTask: timer ended but phone is charging")
        }
        else {
          commandStop()
          d(TAG, "recurringTask: commandStop()")
        }
      }
      else {
        // Timer has not run out, do nothing.
        // d(TAG, "recurringTask: normal");
      }
    }
    updateTile()
  }

  private fun updateTile() {
    val tile = qsTile
    val isRunning = myExecutor != null && !myExecutor!!.isShutdown
    if (tile != null) {
      if (isRunning) {
        setTileToIsRunning(tile)
      }
      else {
        setTitleToIsNotRunning(tile)
      }
      tile.updateTile()
    }
  }

  private fun setTitleToIsNotRunning(tile: Tile) {
    tile.state = Tile.STATE_INACTIVE
    tile.icon = myIconEyeClosed
    tile.label = getString(R.string.tile_inactive_text)
  }

  private fun setTileToIsRunning(tile: Tile) {
    if (isCharging(this)) {
      tile.state = Tile.STATE_ACTIVE
      tile.icon = myIconEyeOpen
      tile.label = getString(R.string.tile_active_charging_text)
    }
    else {
      tile.state = Tile.STATE_ACTIVE
      tile.icon = myIconEyeOpen
      val timeRemaining = MyTileServiceSettings.timeoutNotChargingSec - myTimeRunning_sec
      val formatTime = formatTime(timeRemaining)
      tile.label = getString(R.string.tile_active_text, formatTime)
    }
  }

  private fun formatTime(time_sec: Long): String {
    return if (time_sec <= 60) { // less than 1 min.
      String.format("%ds", time_sec)
    }
    else if (time_sec > 60 && time_sec < 3600) { // less than 60 min.
      val minutes = TimeUnit.SECONDS.toMinutes(time_sec)
      String.format("%dm:%ds", minutes, time_sec - minutes * 60)
    }
    else { // more than 60 min.
      val hours = TimeUnit.SECONDS.toHours(time_sec)
      val minutes = TimeUnit.SECONDS.toMinutes(time_sec)
      String.format(
          "%dh:%dm:%ds",
          hours,
          minutes - hours * 60,
          time_sec - minutes * 60)
    }
  }

  companion object {
    const val TAG = "SA_MyService"
    const val DELAY_INITIAL = 0
    const val DELAY_RECURRING = 1
    val DELAY_UNIT = TimeUnit.SECONDS

    fun startService(context: Context) {
      try {
        context.startService(MyIntentBuilder.getExplicitIntentToStartService(context))
      }
      catch (e: IllegalStateException) {
        // More info: https://developer.android.com/about/versions/oreo/background
        dumpException(e, "Service can't be started, because app is current in background")
        showToast(context, context.getString(R.string.msg_activate_awake_app_manually))
      }
    }

    fun stopService(context: Context) {
      try {
        context.startService(MyIntentBuilder.getExplicitIntentToStopService(context))
      }
      catch (e: IllegalStateException) {
        // More info: https://developer.android.com/about/versions/oreo/background
        dumpException(e, "Service can't be stopped, because app is current in background")
      }
    }

    private fun dumpException(exception: IllegalStateException, message: String) {
      Log.e(TAG, message)
      val buffer: Writer = StringWriter()
      val pw = PrintWriter(buffer)
      exception.printStackTrace(pw)
      Log.e(TAG, buffer.toString())
      pw.close()
    }

    /**
     * @param forceIfNotCharging If you want to start the service regardless of whether the device is currently charging
     * or not then pass true here, otherwise, the service will not start if the device isn't
     * charging.
     */
    fun coldStart(context: Context, forceIfNotCharging: Boolean) {
      // Check if charging, and start it.
      if (forceIfNotCharging || isCharging(context)) {
        startService(context)
      }
    }

    /**
     * More info: https://developer.android.com/reference/android/os/BatteryManager#BATTERY_PLUGGED_AC
     */
    private fun isCharging(context: Context): Boolean {
      val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus = context.applicationContext.registerReceiver(null, intentFilter)
      val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      return status == BatteryManager.BATTERY_STATUS_CHARGING ||
             status == BatteryManager.BATTERY_STATUS_FULL ||
             status == BatteryManager.BATTERY_PLUGGED_AC ||
             status == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
             status == BatteryManager.BATTERY_PLUGGED_USB
    }

    val isPreAndroidO: Boolean
      get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1
  }
}