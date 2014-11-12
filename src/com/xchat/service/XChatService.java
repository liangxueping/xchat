package com.xchat.service;

import java.util.HashSet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import com.xchat.activity.LoginActivity;
import com.xchat.base.BaseService;
import com.xchat.base.IConnectionStatusCallback;
import com.xchat.broadcast.XChatBroadcastReceiver;
import com.xchat.broadcast.XChatBroadcastReceiver.EventHandler;
import com.xchat.utils.NetUtil;
import com.xchat.utils.PreferenceUtil;

public class XChatService extends BaseService implements EventHandler{

	/**
	 * 未连接
	 */
	public static final int DISCONNECTED = -1;
	/**
	 * 已连接
	 */
	public static final int CONNECTED = 0;
	/**
	 * 连接中
	 */
	public static final int CONNECTING = 1;
	/**
	 * 网络错误
	 */
	public static final String NETWORK_ERROR = "network error";
	/**
	 * 手动退出
	 */
	public static final String LOGOUT = "logout";
	/**
	 * 自动重连时间
	 */
	private static final int RECONNECT_AFTER = 5;
	/**
	 * 最大重连时间间隔
	 */
	private static final int RECONNECT_MAXIMUM = 10 * 60;
	
	private int mConnectedState = DISCONNECTED; //是否已经连接
	private int mReconnectTimeout = RECONNECT_AFTER;
	

	private boolean mIsFirstLoginAction;
	
	private Thread mConnectingThread;
	private PendingIntent mPAlarmIntent;
	private IBinder mBinder = new XChatBinder();
	private IConnectionStatusCallback mConnectionStatusCallback;
	private HashSet<String> mIsBoundTo = new HashSet<String>();
	
	@Override
	public IBinder onBind(Intent intent) {
		String chatPartner = intent.getDataString();
		if (chatPartner != null){
			mIsBoundTo.add(chatPartner);
		}
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action) && TextUtils.equals(action, LoginActivity.LOGIN_ACTION)) {
			mIsFirstLoginAction = true;
		} else {
			mIsFirstLoginAction = false;
		}
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		String chatPartner = intent.getDataString();
		if(chatPartner != null){
			mIsBoundTo.add(chatPartner);
		}
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action) && TextUtils.equals(action, LoginActivity.LOGIN_ACTION)) {
			mIsFirstLoginAction = true;
		} else {
			mIsFirstLoginAction = false;
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.remove(chatPartner);
		}
		mIsFirstLoginAction = false;
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		XChatBroadcastReceiver.mListeners.add(this);
//		BaseActivity.mListeners.add(this);
//		mActivityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
//		mPackageName = getPackageName();
//		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));
	}

	@Override
	public void onNetChange() {
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {// 如果是网络断开，不作处理
			connectionFailed(NETWORK_ERROR);
			return;
		}
		if (isAuthenticated()){
			return;
		}
		String account = PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, "");
		String password = PreferenceUtil.getPrefString(PreferenceUtil.PASSWORD, "");
		if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password))// 如果没有帐号，也直接返回
			return;
		if (!PreferenceUtil.getPrefBoolean(PreferenceUtil.SETTING_AUTO_RECONNECT, true))// 不需要重连
			return;
		login(account, password);// 重连
	}
	/**
	 * 注册注解面和聊天界面时连接状态变化回调
	 * 
	 * @param cb
	 */
	public void registerConnectionStatusCallback(IConnectionStatusCallback cb) {
		mConnectionStatusCallback = cb;
	}

	public void unRegisterConnectionStatusCallback() {
		mConnectionStatusCallback = null;
	}
	/**
	 * UI线程反馈连接失败
	 * @param reason
	 */
	private void connectionFailed(String reason) {
		//更新当前连接状态
		mConnectedState = DISCONNECTED;
		//如果是手动退出
		if (TextUtils.equals(reason, LOGOUT)) {
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
			return;
		}
		//回调
		if (mConnectionStatusCallback != null) {
			mConnectionStatusCallback.connectionStatusChanged(mConnectedState, reason);
			//如果是第一次登录,就算登录失败也不需要继续
			if (mIsFirstLoginAction){
				return;
			}
		}

		//无网络连接时,直接返回
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
			return;
		}

		String account = PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, "");
		String password = PreferenceUtil.getPrefString(PreferenceUtil.PASSWORD, "");
		//无保存的帐号密码时，也直接返回
		if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
			return;
		}
		//如果不是手动退出并且需要重新连接，则开启重连闹钟
		if (PreferenceUtil.getPrefBoolean(PreferenceUtil.SETTING_AUTO_RECONNECT, true)) {
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+mReconnectTimeout*1000, mPAlarmIntent);
			mReconnectTimeout = mReconnectTimeout * 2;
			if (mReconnectTimeout > RECONNECT_MAXIMUM){
				mReconnectTimeout = RECONNECT_MAXIMUM;
			}
		} else {
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
		}

	}
	/**
	 * 是否连接上服务器
	 * @return
	 */
	public boolean isAuthenticated() {
//		if (mSmackable != null) {
//			return mSmackable.isAuthenticated();
//		}
		return false;
	}
	/**
	 * 登录
	 * @param account  用户名
	 * @param password 密码
	 */
	public void login(final String account, final String password) {
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
			connectionFailed(NETWORK_ERROR);
			return;
		}
		if (mConnectingThread != null) {
			return;
		}
		mConnectingThread = new Thread() {
			@Override
			public void run() {
//				try {
//					mSmackable = new SmackImpl(XChatService.this);
//					if (mSmackable.login(account, password)) {
//						//登陆成功
//						postConnectionScuessed();
//					} else {
//						//登陆失败
//						postConnectionFailed(LOGIN_FAILED);
//					}
//				} catch (XChatException e) {
//					String message = e.getLocalizedMessage();
//					//登陆失败
//					if (e.getCause() != null)
//						message += "\n" + e.getCause().getLocalizedMessage();
//					postConnectionFailed(message);
//					L.i(XChatService.class, "YaximXMPPException in doConnect():");
//					e.printStackTrace();
//				} finally {
//					if (mConnectingThread != null){
//						synchronized (mConnectingThread) {
//							mConnectingThread = null;
//						}
//					}
//				}
			}

		};
		mConnectingThread.start();
	}

	public class XChatBinder extends Binder {
		public XChatService getService() {
			return XChatService.this;
		}
	}
}
