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

package com.r3bl.stayawake;

import android.annotation.TargetApi;
import android.app.*;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import static com.r3bl.stayawake.MyTileService.getRandomNumber;

public class HandleNotifications {

// Common stuff.

public static final int ONGOING_NOTIFICATION_ID = getRandomNumber();
public static final int SMALL_ICON              = R.drawable.ic_stat_visibility;
public static final int STOP_ACTION_ICON        = R.drawable.ic_stat_flare;

/** PendingIntent to stop the service. */
private static PendingIntent getPendingIntentToStopService(Service context) {
  Intent intentToStopService = new MyIntentBuilder(context).setCommand(Command.STOP).build();
  return PendingIntent.getService(context, getRandomNumber(), intentToStopService, 0);
}

/** Get pending intent to launch the activity. */
private static PendingIntent getPendingIntentToLaunchActivity(Service context) {
  Intent intentToLaunchMainActivity = new Intent(context, MainActivity.class);
  return PendingIntent.getActivity(context, getRandomNumber(), intentToLaunchMainActivity, 0);
}

// Pre O specific.

@TargetApi(25)
public static class PreO {
  public static void createNotification(Service context) {
    // Action to stop the service.
    NotificationCompat.Action stopAction =
        new NotificationCompat.Action.Builder(
            STOP_ACTION_ICON,
            getNotificationStopActionText(context),
            getPendingIntentToStopService(context))
            .build();

    // Create a notification.
    // More information on ignored Channel ID: https://stackoverflow.com/a/45580202/2085356
    Notification notification =
        new NotificationCompat.Builder(context, "")
            .setContentTitle(getNotificationTitle(context))
            .setContentText(getNotificationContent(context))
            .setSmallIcon(SMALL_ICON)
            .setContentIntent(getPendingIntentToLaunchActivity(context))
            .addAction(stopAction)
            .setStyle(new NotificationCompat.BigTextStyle())
            .build();

    context.startForeground(ONGOING_NOTIFICATION_ID, notification);
  }
}

@NonNull
private static String getNotificationContent(Service context) {
  return context.getString(R.string.notification_text_content);
}

@NonNull
private static String getNotificationTitle(Service context) {
  return context.getString(R.string.notification_text_title);
}

// O Specific.

@TargetApi(26)
public static class O {
  public static final String CHANNEL_ID = String.valueOf(getRandomNumber());

  public static void createNotification(Service context) {
    String channelId = createChannel(context);
    Notification notification = buildNotification(context, channelId);
    context.startForeground(ONGOING_NOTIFICATION_ID, notification);
  }

  private static Notification buildNotification(Service context,
                                                String channelId) {
    // Action to stop the service.
    NotificationCompat.Action stopAction =
        new NotificationCompat.Action.Builder(
            STOP_ACTION_ICON,
            getNotificationStopActionText(context),
            getPendingIntentToStopService(context))
            .build();

    // Create a notification.
    return new NotificationCompat.Builder(context, channelId)
               .setContentTitle(getNotificationTitle(context))
               .setContentText(getNotificationContent(context))
               .setSmallIcon(SMALL_ICON)
               .setContentIntent(getPendingIntentToLaunchActivity(context))
               .addAction(stopAction)
               .setStyle(new NotificationCompat.BigTextStyle())
               .build();
  }

  @NonNull
  private static String createChannel(Service context) {
    // Create a channel.
    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    CharSequence channelName = "Playback channel";
    int importance = NotificationManager.IMPORTANCE_DEFAULT;
    NotificationChannel notificationChannel =
        new NotificationChannel(CHANNEL_ID, channelName, importance);
    notificationManager.createNotificationChannel(notificationChannel);
    return CHANNEL_ID;
  }
}

@NonNull
private static String getNotificationStopActionText(Service context) {
  return context.getString(R.string.notification_stop_action_text);
}
} // end class HandleNotifications.
