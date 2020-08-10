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
import androidx.annotation.WorkerThread
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * This is a bound and started service. [TileService] is a bound service, and it automatically binds to the Settings
 * Tile. Since this service also holds a WakeLock, this part of it happens in the started service, which also displays a
 * persistent notification, and takes care of starting itself.
 *
 * If the user has added this quick tile to their notification drawer, then [onCreate] will be called when the device
 * boots. This in turn attaches the [PowerConnectionReceiver] which allows Awake to start automatically when the device
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
  private lateinit var mySettingsHolder: ThreadSafeSettingsHolder

  /** Handle [SettingsChangedEvent] from [EventBus]. */
  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  fun onSettingsChangedEvent(event: MyTileServiceSettings.SettingsChangedEvent) {
    mySettingsHolder.value = event.settings
    d(TAG, "MyTileService.onSettingsChangedEvent: ${mySettingsHolder}")
  }

  // General service code.
  override fun onCreate() {
    super.onCreate()
    myHandler = Handler()
    myIconEyeOpen = Icon.createWithResource(this, R.drawable.ic_stat_visibility)
    myIconEyeClosed = Icon.createWithResource(this, R.drawable.ic_stat_visibility_off)
    myReceiver = PowerConnectionReceiver(this).apply { registerBroadcastReceiver() }
    mySettingsHolder = ThreadSafeSettingsHolder(this)
    MyTileServiceSettings.registerWithEventBus(this)
    //handleAutoStartOfService()
    d(TAG, "onCreate: ")
  }

//  private fun handleAutoStartOfService() {
//    if (mySettings.value.autoStartEnabled && isCharging(this)) {
//      showToast(applicationContext, "MyTileService: autoStartEnabled & isCharging -> auto start service")
//      commandStart()
//      d(TAG, "MyTileService.handleAutoStartOfService: Initiate auto start")
//    }
//    else d(TAG, "MyTileService.handleAutoStartOfService: Do not auto start")
//  }

  override fun onDestroy() {
    super.onDestroy()
    d(TAG, "onDestroy: ")
    myExecutor?.apply {
      shutdownNow()
      d(TAG, "onDestroy: stopping executor")
    }
    updateTile()
    myReceiver?.apply {
      unregisterBroadcastReceiver()
      d(TAG, "unregisterReceiver: PowerConnectionReceiver")
    }
    MyTileServiceSettings.unregisterFromEventBus(this)
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
    when {
      IntentHelper.containsCommand(intent) -> processCommand(IntentHelper.getCommand(intent))
      IntentHelper.containsMessage(intent) -> processMessage(IntentHelper.getMessage(intent))
    }
    return Service.START_NOT_STICKY
  }

  private fun getDebugIntentString(intent: Intent, startId: Int): String {
    val containsCommand = IntentHelper.containsCommand(intent)
    val containsMessage = IntentHelper.containsMessage(intent)
    return String.format(
        "onStartCommand: Service in [%s] state, commandId: [%d], message: [%s], startId: [%d]",
        if (myServiceIsStarted) "STARTED" else "NOT STARTED",
        if (containsCommand) IntentHelper.getCommand(intent) else "N/A",
        if (containsMessage) IntentHelper.getMessage(intent) else "N/A",
        startId)
  }

  private fun processMessage(message: String) {
    try {
      // Do nothing.
      d(TAG, "doMessage: message from client: $message")
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
   * This method can be called directly (by this bound service), or from another Android component by firing an explicit
   * [Intent] with [CommandId.STOP], using [fireIntentWithStopService].
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
   * This method can be called directly, and it will move the service to started state (by firing an explicit [Intent] to
   * [startService]. This happens when this bound service's [onCreate] method causes the service to be started.
   *
   * It is confusing when this method gets called by [fireIntentWithStartService] from other Android components (that
   * are not this bound service). This method is called by [PowerConnectionReceiver] and [MainActivity], and they aren't
   * bound to this service, so they end up generating an [Intent] with [CommandId.STOP] which will put the service in a
   * started state. But then this method itself will also put the already started service in a started state and even
   * show a notification if needed. In this case, it is ok to start a service **twice**, since [myServiceIsStarted] is
   * used to gate this method actually executing multiple times.
   *
   * Unfortunately, this is a really inelegant way to handle this, and stems from the confusing state machine that
   * are Android bound and started services. The alternative would be to have two separate code paths to start a
   * service: 1) from this bound service itself, 2) from other Android components outside of this bound service.
   * However, this would result in the same net effect, which is why this reentrant method does the job with less code.
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
        scheduleWithFixedDelay(::recurringTask, DELAY_INITIAL, DELAY_RECURRING, DELAY_UNIT)
      }
      d(TAG, "commandStart: starting executor")
    }
    else {
      d(TAG, "commandStart: executor not started")
    }
  }

  private fun moveToForegroundAndShowNotification() {
    when {
      isPreAndroidOreo -> HandleNotifications.PreO.createNotification(this)
      else             -> HandleNotifications.O.createNotification(this)
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
    if (isPreAndroidOreo) {
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

  @WorkerThread
  private fun recurringTask() {
    if (isCharging(this)) {
      // Reset the countdown timer.
      myTimeRunning_sec = 0
    }
    else {
      // Run down the countdown timer.
      myTimeRunning_sec += DELAY_UNIT.convert(DELAY_RECURRING, TimeUnit.SECONDS)
      if (myTimeRunning_sec >= mySettingsHolder.value.timeoutNotChargingSec) {
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
    myHandler?.post {
      //if (!isCharging(this)) showToast(this, "🔌🛑🚨️Phone is no longer charging!🔌🛑🚨")
      updateTile()
    }
  }

  @MainThread
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

  private fun setTitleToIsNotRunning(tile: Tile) =
      with(tile) {
        state = Tile.STATE_INACTIVE
        icon = myIconEyeClosed
        label = getString(R.string.tile_inactive_text)
      }

  private fun setTileToIsRunning(tile: Tile) =
      when {
        isCharging(this) -> {
          with(tile) {
            state = Tile.STATE_ACTIVE
            icon = myIconEyeOpen
            label = getString(R.string.tile_active_charging_text)
          }
        }
        else             -> {
          with(tile) {
            state = Tile.STATE_ACTIVE
            icon = myIconEyeOpen
            val timeRemaining = mySettingsHolder.value.timeoutNotChargingSec - myTimeRunning_sec
            val formatTime = formatTime(timeRemaining)
            label = getString(R.string.tile_active_text, formatTime)
          }
        }
      }

  private fun formatTime(time_sec: Long): String =
      when {
        time_sec <= 60       -> { // less than 1 min.
          String.format("%ds", time_sec)
        }
        time_sec in 61..3599 -> { // less than 60 min.
          val minutes = TimeUnit.SECONDS.toMinutes(time_sec)
          String.format("%dm:%ds", minutes, time_sec - minutes * 60)
        }
        else                 -> { // more than 60 min.
          val hours = TimeUnit.SECONDS.toHours(time_sec)
          val minutes = TimeUnit.SECONDS.toMinutes(time_sec)
          String.format("%dh:%dm:%ds", hours, minutes - hours * 60, time_sec - minutes * 60)
        }
      }

  companion object {
    const val TAG = "SA_MyService"
    const val DELAY_INITIAL: Long = 0
    const val DELAY_RECURRING: Long = 5
    val DELAY_UNIT = TimeUnit.SECONDS

    /**
     * Allows other Android components that are not this bound service, to actually put this service into a started
     * state.
     */
    fun fireIntentWithStartService(context: Context) {
      try {
        context.startService(MyIntentBuilder.getExplicitIntentToStartService(context))
      }
      catch (e: IllegalStateException) {
        // More info: https://developer.android.com/about/versions/oreo/background
        logErrorWithStackTrace(e, "Service can't be started, because app is current in background")
        showToast(context, context.getString(R.string.msg_activate_awake_app_manually))
      }
    }

    /**
     * Allows other Android components that are not this bound service, to actually put this service into a stopped
     * state.
     */
    fun fireIntentWithStopService(context: Context) {
      try {
        context.startService(MyIntentBuilder.getExplicitIntentToStopService(context))
      }
      catch (e: IllegalStateException) {
        // More info: https://developer.android.com/about/versions/oreo/background
        logErrorWithStackTrace(e, "Service can't be stopped, because app is current in background")
      }
    }

    private fun logErrorWithStackTrace(exception: IllegalStateException, message: String) {
      Log.e(TAG, message)
      val buffer: Writer = StringWriter()
      val pw = PrintWriter(buffer)
      exception.printStackTrace(pw)
      Log.e(TAG, buffer.toString())
      pw.close()
    }

    /**
     * 1. [Docs](https://developer.android.com/reference/android/os/BatteryManager#BATTERY_PLUGGED_AC)
     * 2. [SO](https://stackoverflow.com/a/11973742/2085356)
     */
    fun isCharging(context: Context): Boolean {
      val intentFilter: IntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryIntent: Intent? = context.applicationContext.registerReceiver(null, intentFilter)

      batteryIntent ?: return false

      val batteryStatus: Int = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      val batteryCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING

      val chargerPlugged: Int = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
      val usbChargerPlugged: Boolean = chargerPlugged == BatteryManager.BATTERY_PLUGGED_USB
      val acChargerPlugged: Boolean = chargerPlugged == BatteryManager.BATTERY_PLUGGED_AC
      val wirelessChargerPlugged: Boolean = chargerPlugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

      return batteryCharging
             || isChargingFromBatteryManager(context) // This is probably redundant w/ batteryCharging.
             || usbChargerPlugged || acChargerPlugged || wirelessChargerPlugged
    }

    /**
     * 1. [Docs](https://developer.android.com/reference/android/os/BatteryManager.html#isCharging())
     * 2. [SO](https://stackoverflow.com/a/47020822/2085356)
     */
    fun isChargingFromBatteryManager(context: Context): Boolean =
        with(context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager) { isCharging }

    val isPreAndroidOreo: Boolean
      get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1
  }
}