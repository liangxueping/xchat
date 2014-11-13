package com.xchat.activity;

import android.app.Activity;
import android.os.Bundle;

public class ChatActivity extends Activity {
	//昵称对应的key
	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class.getName() + ".username";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
}
