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

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.util.LinkifyCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    //showAppIconInActionBar();
    //hideStatusBar();
    loadAndApplyFonts()
    formatMessages()
    MyTileService.coldStart(this, false)
  }

  private fun loadAndApplyFonts() {
    val typeNotoSansRegular = Typeface.createFromAsset(assets, "fonts/notosans_regular.ttf")
    val typeNotoSansBold = Typeface.createFromAsset(assets, "fonts/notosans_bold.ttf")
    val typeTitilumWebLight = Typeface.createFromAsset(assets, "fonts/titilliumweb_light.ttf")
    val typeTitilumWebRegular = Typeface.createFromAsset(assets, "fonts/titilliumweb_regular.ttf")

    listOf<TextView>(text_app_title).forEach { it.typeface = typeNotoSansBold }

    listOf<TextView>(text_marketing_message).forEach { it.typeface = typeTitilumWebLight }

    listOf<TextView>(text_introduction_heading, text_installation_heading, text_opensource_title).forEach {
      it.typeface = typeTitilumWebRegular
    }

    listOf<TextView>(text_introduction_content,
                     text_install_body,
                     text_install_body_1,
                     text_install_body_2,
                     text_install_body_3,
                     text_opensource_body).forEach {
      it.typeface = typeNotoSansRegular
    }
  }

  private fun hideStatusBar() {
    val decorView = window.decorView
    // Hide the status bar.
    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    // Remember that you should never show the action bar if the status bar is hidden, so hide that too if necessary.
    actionBar?.hide()
  }

  private fun showAppIconInActionBar() {
    supportActionBar?.apply {
      setDisplayShowHomeEnabled(true)
      setLogo(R.mipmap.ic_launcher)
      setDisplayUseLogoEnabled(true)
    }
  }

  private fun formatMessages() {
    // Add actual minutes to string template.
    val hours = TimeUnit.SECONDS.toMinutes(MyTileService.MAX_TIME_SEC)
    text_introduction_content.text = getString(R.string.introduction_body, hours)

    // Linkify github link.
    LinkifyCompat.addLinks(text_opensource_body, Linkify.WEB_URLS)

    // Spanning color on textviews.
    applySpan(text_install_body_1, "Step 1")
    applySpan(text_install_body_2, "Step 2")
    applySpan(text_install_body_3, "Step 3")
  }

  private fun applySpan(textView: TextView, substring: String) {
    setColorSpanOnTextView(textView,
                           getString(R.string.install_body_1, substring),
                           substring,
                           getColor(R.color.colorTextDark))
  }

  private fun setColorSpanOnTextView(view: TextView,
                                     fulltext: String,
                                     subtext: String,
                                     color: Int
  ) {
    view.setText(fulltext, TextView.BufferType.SPANNABLE)
    val str = view.text as Spannable
    val i = fulltext.indexOf(subtext)
    str.setSpan(ForegroundColorSpan(color), i, i + subtext.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
  }

  fun buttonStartAwakeClicked(ignore: View) {
    MyTileService.coldStart(this, true)
  }

}