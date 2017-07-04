package com.r3bl.stayawake;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.BatteryManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyTileService extends TileService {

public static final String TAG = "SA_MyService";
private ScheduledExecutorService mExecutor;
private static final int MAX_TIME_SEC = 10;
private int mTimeRunning;

@Override public void onCreate() {
  super.onCreate();
  Log.d(TAG, "onCreate: ");
}

@Override public void onDestroy() {
  super.onDestroy();
  if (mExecutor != null) {
    mExecutor.shutdownNow();
    Log.d(TAG, "onDestroy: stopping executor");
  } else {
    Log.d(TAG, "onDestroy: do nothing");
  }
}

@Override public void onTileAdded() {
  super.onTileAdded();
  Log.d(TAG, "onTileAdded: ");
}

@Override public void onTileRemoved() {
  super.onTileRemoved();
  Log.d(TAG, "onTileRemoved: ");
}

@Override public void onStartListening() {
  super.onStartListening();
  updateTile();
  Log.d(TAG, "onStartListening: ");
}

@Override public void onStopListening() {
  super.onStopListening();
  updateTile();
  Log.d(TAG, "onStopListening: ");
}

@Override public void onClick() {
  super.onClick();
  Log.d(TAG, "onClick: ");
  startService(new Intent(this, MyTileService.class));
}

@Override public int onStartCommand(Intent intent, int flags, int startId) {

  if (mExecutor == null) {
    mTimeRunning = 0;
    mExecutor = Executors.newSingleThreadScheduledExecutor();
    Runnable runnable = new Runnable() {
      @Override public void run() {
        recurringTask();
      }
    };
    mExecutor.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.SECONDS);
    Log.d(TAG, "onStartCommand: starting executor");
  } else {
    Log.d(TAG, "onStartCommand: do nothing");
  }

  return START_NOT_STICKY;
}

private void recurringTask() {

  mTimeRunning++;
  updateTile();

  if (mTimeRunning >= MAX_TIME_SEC) {
    if (!isCharging()) {
      stopSelf();
      Log.d(TAG, "recurringTask: stopSelf");
    } else {
      Log.d(TAG, "recurringTask: timer ended but phone is charging");
    }
  } else {
    Log.d(TAG, "recurringTask: normal");
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