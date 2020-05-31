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

import android.content.Context;
import android.content.Intent;
import androidx.annotation.IntDef;
import junit.framework.Assert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Command enumeration
// more info - http://blog.shamanland.com/2016/02/int-string-enum.html
@IntDef({Command.INVALID, Command.STOP, Command.START})
@Retention(RetentionPolicy.SOURCE) @interface Command {

int INVALID = -1;
int STOP    = 0;
int START   = 1;
}

public class MyIntentBuilder {

private static final String  KEY_MESSAGE = "msg";
private static final String  KEY_COMMAND = "cmd";
private              Context mContext;
private              String  mMessage;
private @Command     int     mCommandId  = Command.INVALID;

public static MyIntentBuilder getInstance(Context context) {
  return new MyIntentBuilder(context);
}

public MyIntentBuilder(Context context) {
  this.mContext = context;
}

public MyIntentBuilder setMessage(String message) {
  this.mMessage = message;
  return this;
}

/**
 * @param command Don't use {@link Command#INVALID} as a param. If you do then
 *                this method does
 *                nothing.
 */
public MyIntentBuilder setCommand(@Command int command) {
  this.mCommandId = command;
  return this;
}

public Intent build() {
  Assert.assertNotNull("Context can not be null!", mContext);
  Intent intent = new Intent(mContext, MyTileService.class);
  if (mCommandId != Command.INVALID) {
    intent.putExtra(KEY_COMMAND, mCommandId);
  }
  if (mMessage != null) {
    intent.putExtra(KEY_MESSAGE, mMessage);
  }
  return intent;
}

public static boolean containsCommand(Intent intent) {
  return intent.getExtras().containsKey(KEY_COMMAND);
}

public static boolean containsMessage(Intent intent) {
  return intent.getExtras().containsKey(KEY_MESSAGE);
}

public static @Command int getCommand(Intent intent) {
  final @Command int commandId = intent.getExtras().getInt(KEY_COMMAND);
  return commandId;
}

public static String getMessage(Intent intent) {
  return intent.getExtras().getString(KEY_MESSAGE);
}
} //end class MyIntentBuilder.
