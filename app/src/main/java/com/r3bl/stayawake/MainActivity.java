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

import android.app.ActionBar;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.text.util.LinkifyCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "SA_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //showAppIconInActionBar();
        //hideStatusBar();
        loadAndApplyFonts();
        formatMessages();
    }

    private void loadAndApplyFonts() {
        Typeface typeNotoSansRegular =
                Typeface.createFromAsset(getAssets(), "fonts/notosans_regular.ttf");
        Typeface typeNotoSansBold =
                Typeface.createFromAsset(getAssets(), "fonts/notosans_bold.ttf");
        Typeface typeTitilumWebLight =
                Typeface.createFromAsset(getAssets(), "fonts/titilliumweb_light.ttf");
        Typeface typeTitilumWebRegular =
                Typeface.createFromAsset(getAssets(), "fonts/titilliumweb_regular.ttf");

        ((TextView) findViewById(R.id.text_app_title))
                .setTypeface(typeNotoSansBold);

        ((TextView) findViewById(R.id.text_marketing_message))
                .setTypeface(typeTitilumWebLight);

        int[] arrayOfTitilumWebRegular = {
                R.id.text_introduction_heading,
                R.id.text_installation_heading,
                R.id.text_opensource_title,
                };

        for (int id : arrayOfTitilumWebRegular) {
            ((TextView) findViewById(id)).setTypeface(typeTitilumWebRegular);
        }

        int[] arrayOfNotoSansRegular = {
                R.id.text_introduction_content,
                R.id.text_install_body,
                R.id.text_install_body_1,
                R.id.text_install_body_2,
                R.id.text_install_body_3,
                R.id.text_opensource_body,
                };

        for (int id : arrayOfNotoSansRegular) {
            ((TextView) findViewById(id)).setTypeface(typeNotoSansRegular);
        }

    }

    private void hideStatusBar() {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    private void showAppIconInActionBar() {
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
    }

    private void formatMessages() {
        // Add actual minutes to string template.
        TextView textView1 = (TextView) findViewById(R.id.text_introduction_content);
        final long hours = TimeUnit.SECONDS.toMinutes(MyTileService.MAX_TIME_SEC);
        textView1.setText(getString(R.string.introduction_body, hours));

        // Linkify github link.
        TextView textview2 = (TextView) findViewById(R.id.text_opensource_body);
        LinkifyCompat.addLinks(textview2, Linkify.WEB_URLS);

        // Spanning color on textviews.
        applySpan((TextView) findViewById(R.id.text_install_body_1),
                  R.id.text_install_body_1,
                  "Step 1");

        applySpan((TextView) findViewById(R.id.text_install_body_2),
                  R.id.text_install_body_2,
                  "Step 2");

        applySpan((TextView) findViewById(R.id.text_install_body_3),
                  R.id.text_install_body_3,
                  "Step 3");

    }

    private void applySpan(TextView textView, int id, String substring) {
        setColorSpanOnTextView(textView,
                               getString(R.string.install_body_1, substring),
                               substring,
                               getColor(R.color.colorTextDark));
    }

    private void setColorSpanOnTextView(TextView view, String fulltext, String subtext, int color) {
        view.setText(fulltext, TextView.BufferType.SPANNABLE);
        Spannable str = (Spannable) view.getText();
        int i = fulltext.indexOf(subtext);
        str.setSpan(new ForegroundColorSpan(color), i, i + subtext.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
