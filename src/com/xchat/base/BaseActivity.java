package com.xchat.base;

import java.util.ArrayList;

import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ksu.xchatandroidcore.OnXChatMessageListener;
import com.ksu.xchatandroidcore.XChatListenerManager;
import com.ksu.xchatcore.nio.model.Message;
import com.ksu.xchatcore.nio.model.ReplyBody;

public class BaseActivity extends FragmentActivity implements OnXChatMessageListener{
	public static ArrayList<BackPressHandler> mListeners = new ArrayList<BackPressHandler>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		XChatListenerManager.registerMessageListener(this,this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mListeners.size() > 0)
			for (BackPressHandler handler : mListeners) {
				handler.activityOnResume();
			}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mListeners.size() > 0)
			for (BackPressHandler handler : mListeners) {
				handler.activityOnPause();
			}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		XChatListenerManager.removeMessageListener(this);
	};

	public static abstract interface BackPressHandler {

		public abstract void activityOnResume();

		public abstract void activityOnPause();

	}

	@Override
	public void onMessageReceived(Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReplyReceived(ReplyBody replybody) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNetworkChanged(NetworkInfo networkinfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionStatus(boolean isConnected) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionSucceed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionClosed() {
		// TODO Auto-generated method stub
		
	}
}
