package com.r3bl.stayawake;

import android.os.Bundle;
import android.support.v4.text.util.LinkifyCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

public static final String TAG = "SA_MainActivity";

@Override
protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_main);
  TextView textView = (TextView) findViewById(R.id.textview);
  LinkifyCompat.addLinks(textView, Linkify.WEB_URLS);
}
}
