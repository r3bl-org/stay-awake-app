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

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import com.r3bl.stayawake.CommandId.Companion.START
import com.r3bl.stayawake.CommandId.Companion.STOP
import org.junit.Assert

/**
 * Command enumeration using [IntDef]. More info:
 * - http://blog.shamanland.com/2016/02/int-string-enum.html
 * - https://stackoverflow.com/a/42486280/2085356
 */
@IntDef(CommandId.INVALID, STOP, START)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class CommandId {
  companion object {
    const val INVALID = -1
    const val STOP = 0
    const val START = 1
  }
}

class MyIntentBuilder(private val context: Context?) {
  private data class Command(@CommandId var id: Int = CommandId.INVALID, var message: String? = null)

  private val command: Command = Command()

  fun setMessage(message: String): MyIntentBuilder = this.apply { command.message = message }

  /**
   * @param id Don't use [CommandId.INVALID] as a param. If you do then this method does nothing.
   */
  fun setCommand(@CommandId id: Int): MyIntentBuilder = this.apply { command.id = id }

  fun build(): Intent {
    Assert.assertNotNull("Context can not be null!", context)
    val intent = Intent(context, MyTileService::class.java)
    if (command.id != CommandId.INVALID) intent.putExtra(KEY_COMMAND_ID, command.id)
    if (command.message != null) intent.putExtra(KEY_MESSAGE, command.message)
    return intent
  }

  companion object {
    private const val KEY_MESSAGE = "msg"
    private const val KEY_COMMAND_ID = "cmd_id"

    // Builder.
    fun getInstance(context: Context?): MyIntentBuilder = MyIntentBuilder(context)

    // Intent helpers.
    fun containsCommand(intent: Intent): Boolean = intent.extras.containsKey(KEY_COMMAND_ID)
    fun containsMessage(intent: Intent): Boolean = intent.extras.containsKey(KEY_MESSAGE)

    @CommandId
    fun getCommand(intent: Intent): Int = intent.extras.getInt(KEY_COMMAND_ID)
    fun getMessage(intent: Intent): String = intent.extras.getString(KEY_MESSAGE)

    // Helper methods.
    fun getExplicitIntentToStartService(context: Context?) = getInstance(context).setCommand(START).build()

    fun getExplicitIntentToStopService(context: Context?) = getInstance(context).setCommand(STOP).build()
  }

} //end class MyIntentBuilder.
