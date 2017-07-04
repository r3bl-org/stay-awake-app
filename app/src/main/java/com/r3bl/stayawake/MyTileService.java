package com.r3bl.stayawake;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.BatteryManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.v4.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.util.Log.d;
import static android.util.Log.e;
import static com.r3bl.stayawake.Utils.getRandomNumber;

public class MyTileService extends TileService {

public static final  String TAG                     = "SA_MyService";
private static final int    ONGOING_NOTIFICATION_ID = Utils.getRandomNumber();
private ScheduledExecutorService mExecutor;
private static final int MAX_TIME_SEC = 10;
private int mTimeRunning;

@Override public void onCreate() {
  super.onCreate();
  d(TAG, "onCreate: ");
}

@Override public void onDestroy() {
  super.onDestroy();
  if (mExecutor != null) {
    mExecutor.shutdownNow();
    d(TAG, "onDestroy: stopping executor");
  } else {
    d(TAG, "onDestroy: do nothing");
  }
  updateTile();
}

@Override public void onTileAdded() {
  super.onTileAdded();
  d(TAG, "onTileAdded: ");
  updateTile();
}

@Override public void onTileRemoved() {
  super.onTileRemoved();
  d(TAG, "onTileRemoved: ");
}

@Override public void onStartListening() {
  super.onStartListening();
  d(TAG, "onStartListening: ");
  updateTile();
}

@Override public void onStopListening() {
  super.onStopListening();
  d(TAG, "onStopListening: ");
  updateTile();
}

@Override public void onClick() {
  super.onClick();
  d(TAG, "onClick: starting service with Command.START");
  startService(new MyIntentBuilder(this).setCommand(Command.START).build());
  updateTile();
}

@Override public int onStartCommand(Intent intent, int flags, int startId) {
  d(TAG, String.format("onStartCommand: startId: '%d'", startId));
  routeIntentToCommand(intent);
  return START_NOT_STICKY;
}

private void routeIntentToCommand(Intent intent) {
  if (intent != null) {

    // process command
    if (MyIntentBuilder.containsCommand(intent)) {
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
    d(TAG,
      String.format("doMessage: message from client: '%s'",
                    message));

  } catch (Exception e) {
    e(TAG, "processMessage: exception", e);
  }
}

private void processCommand(int command) {
  try {
    switch (command) {
      case Command.SELF_START:
        commandSelfStart();
        break;
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
 * This is a no-op
 */
private void commandSelfStart() {
}

private void commandStop() {
  releaseWakeLock();
  stopForeground(true);
  stopSelf();
}

private void commandStart() {

  // self start the service just to move the service to a started state.
  // self starting is just to set the status, it is a no-op otherwise.
  startService(MyIntentBuilder.getInstance(this).setCommand(Command.SELF_START).build());

  if (mExecutor == null) {
    mTimeRunning = 0;
    moveServiceToForeground();
    acquireWakeLock();
    mExecutor = Executors.newSingleThreadScheduledExecutor();
    Runnable runnable = new Runnable() {
      @Override public void run() {
        recurringTask();
      }
    };
    mExecutor.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.SECONDS);
    d(TAG, "onStartCommand: starting executor");
  } else {
    d(TAG, "onStartCommand: do nothing");
  }

}

private void moveServiceToForeground() {

  // PendingIntent to launch the activity.
  PendingIntent piLaunchMainActivity;
  {
    Intent iLaunchMainActivity = new Intent(this, MainActivity.class);
    piLaunchMainActivity = PendingIntent.getActivity(
      this, getRandomNumber(), iLaunchMainActivity, 0);
  }

  // PendingIntent to stop the service.
  PendingIntent piStopService;
  {
    Intent iStopService = new MyIntentBuilder(this).setCommand(Command.STOP).build();
    piStopService = PendingIntent.getService(
      this, getRandomNumber(), iStopService, 0);
  }

  // Action to stop the service.
  NotificationCompat.Action stopAction =
    new NotificationCompat.Action.Builder(R.drawable.ic_stat_flare,
                                          getString(R.string.stop_action_text),
                                          piStopService
    ).build();

  Notification mNotification =
    new NotificationCompat.Builder(this)
      .setContentTitle(getString(R.string.notification_title_text))
      .setContentText(getString(R.string.notification_content_text))
      .setSmallIcon(R.drawable.ic_stat_whatshot)
      .setContentIntent(piLaunchMainActivity)
      .addAction(stopAction)
      .setStyle(new NotificationCompat.BigTextStyle())
      .build();

  startForeground(ONGOING_NOTIFICATION_ID, mNotification);

  d(TAG, "moveServiceToForeground: 1) notification created, 2) service in foreground, 3) MP started");

}

private void acquireWakeLock() {
  d(TAG, "acquireWakeLock: ");
}

private void releaseWakeLock() {
  d(TAG, "releaseWakeLock: ");
}

private void recurringTask() {

  mTimeRunning++;
  updateTile();

  if (mTimeRunning >= MAX_TIME_SEC) {
    if (!isCharging()) {
      stopSelf();
      d(TAG, "recurringTask: stopSelf");
    } else {
      d(TAG, "recurringTask: timer ended but phone is charging");
    }
  } else {
    d(TAG, "recurringTask: normal");
  }

}

private void updateTile() {
  boolean isRunning = (mExecutor != null);
  Tile tile = getQsTile();
  if (tile != null) {
    if (isRunning) {
      //tile.getIcon().setTint(Color.RED); // doesn't do anything
      tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_visibility));
      tile.setState(Tile.STATE_ACTIVE);
      tile.setLabel(String.format("%s - %ds",
                                  getString(R.string.tile_active_text),
                                  mTimeRunning));
    } else {
      //tile.getIcon().setTint(Color.WHITE); // doesn't do anything
      tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_visibility_off));
      tile.setState(Tile.STATE_INACTIVE);
      tile.setLabel(getString(R.string.tile_inactive_text));
    }
    tile.updateTile();
  }
}

private boolean isCharging() {
  IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
  Intent batteryStatus = getApplicationContext().registerReceiver(null, intentFilter);
  int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
  boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL;
  return isCharging;
}

}//end class MyTileService.