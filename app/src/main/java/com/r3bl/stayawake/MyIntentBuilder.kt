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
import com.r3bl.stayawake.CommandId.Companion.INVALID
import com.r3bl.stayawake.CommandId.Companion.START
import com.r3bl.stayawake.CommandId.Companion.STOP
import org.junit.Assert

/**
 * Command enumeration using [IntDef].
 * More info:
 * - http://blog.shamanland.com/2016/02/int-string-enum.html
 * - https://stackoverflow.com/a/42486280/2085356
 */
@IntDef(INVALID, STOP, START)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class CommandId {
  companion object {
    const val INVALID = -1
    const val STOP = 0
    const val START = 1
  }
}

private enum class Keys { Message, CommandId }

private data class Command(@CommandId val id: Int, val message: String?) {
  fun toIntent(context: Context): Intent {
    val intent = Intent(context, MyTileService::class.java)
    if (id != INVALID) intent.putExtra(Keys.CommandId.name, id)
    if (message != null) intent.putExtra(Keys.Message.name, message)
    return intent
  }
}

class MyIntentBuilder {
  @CommandId
  var commandId: Int = INVALID

  /** This can't be null when [build] is called. */
  var context: Context? = null

  /** This can be null. */
  var message: String? = null

  fun build(): Intent {
    Assert.assertNotNull("Context can not be null!", context)
    return Command(commandId, message).toIntent(context!!)
  }

  companion object {
    // Helper methods.
    fun getExplicitIntentToStartService(ctx: Context) = buildIntent {
      context = ctx
      commandId = START
    }

    fun getExplicitIntentToStopService(ctx: Context) = buildIntent {
      context = ctx
      commandId = STOP
    }
  }

}

/** Simple DSL for builder. */
fun buildIntent(block: MyIntentBuilder.() -> Unit): Intent = MyIntentBuilder().apply(block).build()

// Intent helpers.
class IntentHelper {
  companion object {
    fun containsCommand(intent: Intent): Boolean = intent.extras.containsKey(Keys.CommandId.name)
    fun containsMessage(intent: Intent): Boolean = intent.extras.containsKey(Keys.Message.name)

    @CommandId
    fun getCommand(intent: Intent): Int = intent.extras.getInt(Keys.CommandId.name)
    fun getMessage(intent: Intent): String = intent.extras.getString(Keys.Message.name)
  }
}
