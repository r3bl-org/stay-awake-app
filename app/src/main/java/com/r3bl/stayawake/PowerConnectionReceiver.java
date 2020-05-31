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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import static android.util.Log.d;
import static com.r3bl.stayawake.MyTileService.TAG;

/** Changes to Android broadcast receiver behaviors: http://tinyurl.com/y9rm5wzg */
public class PowerConnectionReceiver extends BroadcastReceiver {

private Context context;

public void onReceive(Context context, Intent intent) {
  this.context = context;
  String action = intent.getAction();
  String msg1 = "onReceive: Action=" + action;
  d(TAG, msg1);
  switch (action) {
    case Intent.ACTION_POWER_CONNECTED:
      powerConnected();
      break;
    case Intent.ACTION_POWER_DISCONNECTED:
      powerDisconnected();
      break;
    default:
      showToast(msg1);
      break;
  }
}

// Do nothing when power disconnected.
private void powerDisconnected() {
  String msg1 = "PowerConnectionReceiver onReceive(): ACTION_POWER_DISCONNECTED ... Do nothing";
  showToast(msg1);
  d(TAG, msg1);
}

// Start service when power connected.
private void powerConnected() {
  Intent startServiceIntent = new MyIntentBuilder(context).setCommand(Command.START).build();
  context.startService(startServiceIntent);
  String msg1 = "PowerConnectionReceiver onReceive(): ACTION_POWER_CONNECTED ... Start Service";
  showToast(msg1);
  d(TAG, msg1);
}

private void showToast(String msg1) {
  Toast.makeText(context, msg1, Toast.LENGTH_LONG).show();
}
}
