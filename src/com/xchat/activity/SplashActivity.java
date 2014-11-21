package com.xchat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.ksu.xchatandroidcore.XChatPushManager;
import com.xchat.base.BaseActivity;
import com.xchat.utils.PreferenceUtil;

public class SplashActivity extends BaseActivity {
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		//连接服务端
		XChatPushManager.init(SplashActivity.this, PreferenceUtil.HOST_SERVER, PreferenceUtil.HOST_PORT);
	}

	@Override
	public void onConnectionSucceed() {
		mHandler = new Handler();
		String password = PreferenceUtil.getPrefString(PreferenceUtil.PASSWORD, "");
		if (!TextUtils.isEmpty(password)) {
			mHandler.postDelayed(gotoMainAct, 1000);
		} else {
			mHandler.postDelayed(gotoLoginAct, 1000);
		}
	}
	

	Runnable gotoLoginAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, LoginActivity.class));
			finish();
		}
	};

	Runnable gotoMainAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, MainActivity.class));
			finish();
		}
	};
}
