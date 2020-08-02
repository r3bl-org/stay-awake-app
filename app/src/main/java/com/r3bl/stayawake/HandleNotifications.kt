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
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.r3bl.stayawake.MyIntentBuilder.Companion.getExplicitIntentToStopService
import java.util.*

object HandleNotifications {
  // Common stuff.
  val ONGOING_NOTIFICATION_ID = randomNumber
  const val SMALL_ICON = R.drawable.ic_stat_visibility
  const val STOP_ACTION_ICON = R.drawable.ic_stat_flare

  /** [PendingIntent] to stop the service. */
  private fun getPendingIntentToStopService(context: Service) =
      PendingIntent.getService(context, randomNumber, getExplicitIntentToStopService(context), 0)

  /** Get [PendingIntent] to launch the activity. */
  private fun getPendingIntentToLaunchActivity(context: Service): PendingIntent {
    val intentToLaunchMainActivity = Intent(context, MainActivity::class.java)
    return PendingIntent.getActivity(context, randomNumber, intentToLaunchMainActivity, 0)
  }

  private fun getContent(context: Service) = context.getString(R.string.notification_text_content)

  private fun getTitle(context: Service) = context.getString(R.string.notification_text_title)

  private fun getStopActionText(context: Service) = context.getString(R.string.notification_stop_action_text)

  // Pre O specific.
  @TargetApi(Build.VERSION_CODES.N_MR1)
  object PreO {
    fun createNotification(context: Service) {
      // Action to stop the service.
      val stopAction = NotificationCompat.Action.Builder(
          STOP_ACTION_ICON,
          getStopActionText(context),
          getPendingIntentToStopService(context))
          .build()
      // Create a notification.
      // More information on ignored Channel ID: https://stackoverflow.com/a/45580202/2085356
      val notification = NotificationCompat.Builder(context, "")
          .setContentTitle(getTitle(context))
          .setContentText(getContent(context))
          .setSmallIcon(SMALL_ICON)
          .setContentIntent(getPendingIntentToLaunchActivity(context))
          .addAction(stopAction)
          .setStyle(NotificationCompat.BigTextStyle())
          .build()
      context.startForeground(ONGOING_NOTIFICATION_ID, notification)
    }
  }

  // O Specific.
  @TargetApi(Build.VERSION_CODES.O)
  object O {
    fun createNotification(context: Service) {
      val channelId = createChannel(context)
      val notification = buildNotification(context, channelId)
      context.startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(context: Service, channelId: String): Notification {
      // Action to stop the service.
      val stopAction = NotificationCompat.Action.Builder(
          STOP_ACTION_ICON,
          getStopActionText(context),
          getPendingIntentToStopService(context))
          .build()
      // Create a notification.
      return NotificationCompat.Builder(context, channelId)
          .setContentTitle(getTitle(context))
          .setContentText(getContent(context))
          .setSmallIcon(SMALL_ICON)
          .setContentIntent(getPendingIntentToLaunchActivity(context))
          .addAction(stopAction)
          .setStyle(NotificationCompat.BigTextStyle())
          .build()
    }

    private fun createChannel(context: Service): String {
      // Create a channel.
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val id = context.getString(R.string.notification_channel_id)
      val name = context.getString(R.string.notification_channel_name)
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      notificationManager.createNotificationChannel(NotificationChannel(id, name, importance))
      return id
    }
  }

  private val randomNumber: Int
    get() = Random().nextInt(100000)

}
