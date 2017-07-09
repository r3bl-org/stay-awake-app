/*
 * Copyright 2017 R3BL LLC.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.r3bl.stayawake;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.util.Log.d;
import static android.util.Log.e;
import static com.r3bl.stayawake.MyIntentBuilder.containsCommand;

/**
 * This is a bound and started service. TileService is a bound service, and it automatically binds
 * to the Settings Tile. Since this service also holds a WakeLock, this part of it happens in the
 * started service, which also displays a persistent notification, and takes care of starting
 * itself.
 */
public class MyTileService extends TileService {

// Constants.

public static final String   TAG             = "SA_MyService";
public static final long     MAX_TIME_SEC    = TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);
public static final int      DELAY_INITIAL   = 0;
public static final int      DELAY_RECURRING = 1;
public static final TimeUnit DELAY_UNIT      = TimeUnit.SECONDS;

// Data.

private long                     mTimeRunning_sec;
private PowerManager.WakeLock    wakeLock;
private ScheduledExecutorService mExecutor;
private boolean                  mServiceIsStarted;

// General service code.

@Override
public void onCreate() {
  super.onCreate();
  d(TAG, "onCreate: ");
}

@Override
public void onDestroy() {
  super.onDestroy();
  if (mExecutor != null) {
    mExecutor.shutdownNow();
    d(TAG, "onDestroy: stopping executor");
  } else {
    d(TAG, "onDestroy: do nothing");
  }
  updateTile();
}

// Bound Service code & TileService code.

@Override
public void onTileAdded() {
  super.onTileAdded();
  d(TAG, "onTileAdded: ");
  updateTile();
}

@Override
public void onTileRemoved() {
  super.onTileRemoved();
  d(TAG, "onTileRemoved: ");
}

@Override
public void onStartListening() {
  super.onStartListening();
  d(TAG, "onStartListening: ");
  updateTile();
}

@Override
public void onStopListening() {
  super.onStopListening();
  d(TAG, "onStopListening: ");
  updateTile();
}

@Override
public void onClick() {
  super.onClick();

  if (mServiceIsStarted) {
    d(TAG, "onClick: calling commandStop()");
    commandStop();
  } else {
    d(TAG, "onClick: calling commandStart()");
    commandStart();
  }

  updateTile();
}

// Started service code.

@Override
public int onStartCommand(Intent intent, int flags, int startId) {
  boolean containsCommand = MyIntentBuilder.containsCommand(intent);
  d(
    TAG,
    String.format(
      "onStartCommand: Service in [%s] state. commandId: [%d]. startId: [%d]",
      mServiceIsStarted ? "STARTED" : "NOT STARTED",
      containsCommand ? MyIntentBuilder.getCommand(intent) : "N/A",
      startId));
  mServiceIsStarted = true;
  routeIntentToCommand(intent);
  return START_NOT_STICKY;
}

private void routeIntentToCommand(Intent intent) {
  if (intent != null) {

    // process command
    if (containsCommand(intent)) {
      processCommand(MyIntentBuilder.getCommand(intent));
    }

    // process message
    if (MyIntentBuilder.containsMessage(intent)) {
      processMessage(MyIntentBuilder.getMessage(intent));
    }
  }
}

private void processMessage(String message) {
  try {
    d(TAG, String.format("doMessage: message from client: '%s'", message));

  } catch (Exception e) {
    e(TAG, "processMessage: exception", e);
  }
}

private void processCommand(int command) {
  try {
    switch (command) {
      case Command.START:
        commandStart();
        break;
      case Command.STOP:
        commandStop();
        break;
    }
  } catch (Exception e) {
    e(TAG, "processCommand: exception", e);
  }
}

/**
 * This method can be called directly, or by firing an explicit Intent with {@link
 * Command#STOP}.
 */
private void commandStop() {
  releaseWakeLock();
  stopForeground(true);
  stopSelf();
  mServiceIsStarted = false;
  mExecutor.shutdown();
  mExecutor = null;
  updateTile();
}

/**
 * This can be called via an explicit intent to start this serivce, which calls {@link
 * #onStartCommand(Intent, int, int)} or it can be called directly, which is what happens in
 * {@link #onClick()} by this bound service.
 * <p>
 * <p>This is why the service needs to {@link #moveToStartedState()} if it's not already in a
 * started state. More details can be found in the method documentation itself.
 */
private void commandStart() {

  if (!mServiceIsStarted) {
    moveToStartedState();
    return;
  }

  if (mExecutor == null) {
    mTimeRunning_sec = 0;

    if (isPreAndroidO()) {
      HandleNotifications.PreO.createNotification(this);
    } else {
      HandleNotifications.O.createNotification(this);
    }

    acquireWakeLock();
    mExecutor = Executors.newSingleThreadScheduledExecutor();
    Runnable runnable =
      new Runnable() {
        @Override
        public void run() {
          recurringTask();
        }
      };
    mExecutor.scheduleWithFixedDelay(runnable, DELAY_INITIAL, DELAY_RECURRING, DELAY_UNIT);
    d(TAG, "commandStart: starting executor");
  } else {
    d(TAG, "commandStart: do nothing");
  }
}

/**
 * If a call is made to {@link #commandStart()} without firing an explicit Intent to put this
 * service in a started state (which happens in {@link #onClick()}), then fire the explicit
 * intent with {@link Command#START} which actually ends up calling {@link #commandStart()}
 * again and this time, does the work of creating the executor.
 * <p>
 * <p>Next, you would move this service into the foreground, which you can't do unless this
 * service is in a started state.
 */
@TargetApi(Build.VERSION_CODES.O)
private void moveToStartedState() {

  Intent intent = new MyIntentBuilder(this).setCommand(Command.START).build();
  if (isPreAndroidO()) {
    Log.d(TAG, "moveToStartedState: Running on Android N or lower - startService(intent)");
    startService(intent);
  } else {
    Log.d(TAG, "moveToStartedState: Running on Android O - startForegroundService(intent)");
    startForegroundService(intent);
  }
}

private void acquireWakeLock() {
  if (wakeLock == null) {
    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
    wakeLock.acquire();
    d(TAG, "acquireWakeLock: ");
  }
}

private void releaseWakeLock() {
  if (wakeLock != null) {
    wakeLock.release();
    wakeLock = null;
    d(TAG, "releaseWakeLock: ");
  }
}

private void recurringTask() {

  mTimeRunning_sec++;

  if (mTimeRunning_sec >= MAX_TIME_SEC) {
    // Timer has run out.
    if (isCharging()) {
      d(TAG, "recurringTask: timer ended but phone is charging");
    } else {
      commandStop();
      d(TAG, "recurringTask: commandStop()");
    }
  } else {
    // Timer has not run out, do nothing.
    //d(TAG, "recurringTask: normal");
  }

  updateTile();
}

private void updateTile() {

  Tile tile = getQsTile();

  boolean isRunning = (mExecutor != null && !mExecutor.isShutdown());
  if (tile != null) {
    if (isRunning) {
      tile.setState(Tile.STATE_ACTIVE);
      tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_visibility));
      tile.setLabel(getString(R.string.tile_active_text, formatTime(mTimeRunning_sec)));
    } else {
      tile.setState(Tile.STATE_INACTIVE);
      tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_visibility_off));
      tile.setLabel(getString(R.string.tile_inactive_text));
    }
  }

  tile.updateTile();
}

private String formatTime(long time_sec) {
  if (time_sec <= 60) { // less than 1 min.

    return String.format("%ds", time_sec);

  } else if (time_sec > 60 && time_sec < 3600) { //less than 60 min.

    final long minutes = TimeUnit.SECONDS.toMinutes(time_sec);
    return String.format("%dm:%ds", minutes, time_sec - (minutes * 60));

  } else { // more than 60 min.

    final long hours = TimeUnit.SECONDS.toHours(time_sec);
    final long minutes = TimeUnit.SECONDS.toMinutes(time_sec);
    return String.format(
      "%dh:%dm:%ds", hours, minutes - (hours * 60), time_sec - (minutes * 60));
  }
}

private boolean isCharging() {
  IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
  Intent batteryStatus = getApplicationContext().registerReceiver(null, intentFilter);
  int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
  boolean isCharging =
    status == BatteryManager.BATTERY_STATUS_CHARGING
    || status == BatteryManager.BATTERY_STATUS_FULL;
  return isCharging;
}

public static int getRandomNumber() {
  return new Random().nextInt(100000);
}

public static boolean isPreAndroidO() {
  return Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1;
}
} //end class MyTileService.
