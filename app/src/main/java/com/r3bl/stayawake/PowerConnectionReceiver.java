/*
 * Copyright 2019 R3BL LLC. All rights reserved.
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
import android.util.Log;

/** Changes to Android broadcast receiver behaviors: http://tinyurl.com/y9rm5wzg */
public class PowerConnectionReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Log.d(MyTileService.TAG, "");
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            // Start service when power connected.
            Log.d(MyTileService.TAG, "PowerConnectionReceiver onReceive(): ACTION_POWER_CONNECTED");
            Log.d(MyTileService.TAG, "Start Service");
            Intent iStartService = new MyIntentBuilder(context).setCommand(Command.START).build();
            context.startService(iStartService);
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            // Do nothing when power disconnected.
            Log.d(
                    MyTileService.TAG,
                    "PowerConnectionReceiver onReceive(): ACTION_POWER_DISCONNECTED");
            Log.d(MyTileService.TAG, "Do nothing");
        }
    }
}
