package com.xchat.activity;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class ChatActivity extends Activity {
	//昵称对应的key
	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class.getName() + ".username";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
