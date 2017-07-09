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

import android.os.Bundle;
import android.support.v4.text.util.LinkifyCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "SA_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //showAppIconInActionBar();
        setContentView(R.layout.activity_main);
        formatMessage();
    }

    private void showAppIconInActionBar() {
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
    }

    private void formatMessage() {
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        TextView textview2 = (TextView) findViewById(R.id.textview2);

        final long hours = TimeUnit.SECONDS.toMinutes(MyTileService.MAX_TIME_SEC);
        textView1.setText(getString(R.string.about_message, hours));

        LinkifyCompat.addLinks(textview2, Linkify.WEB_URLS);
    }
}
