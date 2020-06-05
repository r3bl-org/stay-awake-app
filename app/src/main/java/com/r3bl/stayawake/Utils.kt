@file:JvmName("Utils")
package com.r3bl.stayawake

import android.content.Context
import android.widget.Toast

fun showToast(context: Context, msg1: String) {
  Toast.makeText(context, msg1, Toast.LENGTH_LONG).show()
}
