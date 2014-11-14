package com.xchat.service;

import java.util.HashSet;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import com.xchat.activity.LoginActivity;
import com.xchat.activity.MainActivity;
import com.xchat.activity.R;
import com.xchat.base.BaseActivity;
import com.xchat.base.BaseActivity.BackPressHandler;
import com.xchat.base.BaseService;
import com.xchat.broadcast.XChatBroadcastReceiver;
import com.xchat.broadcast.XChatBroadcastReceiver.EventHandler;
import com.xchat.dao.IXChatDao;
import com.xchat.dao.SmackDao;
import com.xchat.db.ChatProvider;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.utils.NetUtil;
import com.xchat.utils.PreferenceUtil;

public class XChatService extends BaseService implements EventHandler,BackPressHandler{

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
	 * 登录失败
	 */
	public static final String LOGIN_FAILED = "login failed";
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
	
	private static final String RECONNECT_ALARM = "com.xchat.RECONNECT_ALARM";
	
	private int mConnectedState = DISCONNECTED; //是否已经连接
	private int mReconnectTimeout = RECONNECT_AFTER;
	private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
	

	private boolean mIsFirstLoginAction;

	private IXChatDao xChatDao;
	private Thread mConnectingThread;
	private PendingIntent mPAlarmIntent;
	private IBinder mBinder = new XChatBinder();
	private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
	private IConnectionStatusCallback mConnectionStatusCallback;
	private HashSet<String> mIsBoundTo = new HashSet<String>();
	private Handler mMainHandler = new Handler();
	private ActivityManager mActivityManager;
	

	/**
	 * 判断程序是否在后台运行的任务
	 */
	Runnable monitorStatus = new Runnable() {
		public void run() {
			try {
				mMainHandler.removeCallbacks(monitorStatus);
				// 如果在后台运行并且连接上了
				if (!isAppOnForeground()) {
					updateServiceNotification(getString(R.string.tip_running_background));
					return;
				} else {
					stopForeground(true);
				}
				// mMainHandler.postDelayed(monitorStatus, 1000L);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null
				&& intent.getAction() != null
				&& TextUtils.equals(intent.getAction(), XChatBroadcastReceiver.BOOT_COMPLETED_ACTION)) {
			String account = PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, "");
			String password = PreferenceUtil.getPrefString(PreferenceUtil.PASSWORD, "");
			if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(password)){
				login(account, password);
			}
		}
		mMainHandler.removeCallbacks(monitorStatus);
		//检查应用是否在后台运行线程
		mMainHandler.postDelayed(monitorStatus, 1000L);
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		XChatBroadcastReceiver.mListeners.add(this);
		BaseActivity.mListeners.add(this);
		mActivityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		XChatBroadcastReceiver.mListeners.remove(this);
		BaseActivity.mListeners.remove(this);
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);// 取消重连闹钟
		unregisterReceiver(mAlarmReceiver);// 注销广播监听
		logout();
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

	@Override
	public void activityOnResume() {
		mMainHandler.post(monitorStatus);
	}

	@Override
	public void activityOnPause() {
		mMainHandler.postDelayed(monitorStatus, 1000L);
	}
	/**
	 * 更新通知栏
	 * 
	 * @param message
	 */
	@SuppressWarnings("deprecation")
	public void updateServiceNotification(String message) {
		if (!PreferenceUtil.getPrefBoolean(PreferenceUtil.SETTING_FOREGROUND, true)){
			return;
		}
		String title = PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, "");
		Notification n = new Notification(R.drawable.login_default_avatar, title, System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		n.setLatestEventInfo(this, title, message, n.contentIntent);
		startForeground(SERVICE_NOTIFICATION, n);
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
	public boolean isAppOnForeground() {
		List<RunningTaskInfo> taskInfos = mActivityManager.getRunningTasks(1);
		if (taskInfos.size() > 0 && TextUtils.equals(getPackageName(), taskInfos.get(0).topActivity.getPackageName())) {
			return true;
		}
		return false;
	}
	private void connectionScuessed() {
		//已经连接上
		mConnectedState = CONNECTED;
		//重置重连的时间
		mReconnectTimeout = RECONNECT_AFTER;
		if (mConnectionStatusCallback != null){
			mConnectionStatusCallback.connectionStatusChanged(mConnectedState, "");
		}
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
		if (xChatDao != null) {
			return xChatDao.isAuthenticated();
		}
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
				xChatDao = new SmackDao(XChatService.this);
				if (xChatDao.login(account, password)) {
					//登陆成功
					mMainHandler.post(new Runnable() {
						public void run() {
							connectionScuessed();
						}
					});
				} else {
					//登陆失败
					mMainHandler.post(new Runnable() {
						public void run() {
							connectionFailed(LOGIN_FAILED);
						}
					});
				}
			}

		};
		mConnectingThread.start();
	}
	/**
	 * 退出
	 * @return
	 */
	public boolean logout() {
		boolean isLogout = false;
		if (mConnectingThread != null) {
			synchronized (mConnectingThread) {
				try {
					mConnectingThread.interrupt();
					mConnectingThread.join(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					mConnectingThread = null;
				}
			}
		}
		if (xChatDao != null) {
			isLogout = xChatDao.logout();
			xChatDao = null;
		}
		connectionFailed(LOGOUT);// 手动退出
		return isLogout;
	}

	/**
	 * 新增联系人
	 * @param user
	 * @param alias
	 * @param group
	 */
	public void addRosterItem(String user, String alias, String group) {
//		try {
//			xChatDao.addRosterItem(user, alias, group);
//		} catch (XChatException e) {
//			T.showShort(this, e.getMessage());
//			L.e("exception in addRosterItem(): " + e.getMessage());
//		}
	}

	/**
	 * 删除联系人
	 * @param user
	 */
	public void removeRosterItem(String user) {
//		try {
//			xChatDao.removeRosterItem(user);
//		} catch (XChatException e) {
//			T.showShort(this, e.getMessage());
//			L.e("exception in removeRosterItem(): " + e.getMessage());
//		}
	}

	/**
	 * 将联系人移动到其他组
	 * @param user
	 * @param group
	 */
	public void moveRosterItemToGroup(String user, String group) {
//		try {
//			xChatDao.moveRosterItemToGroup(user, group);
//		} catch (XChatException e) {
//			T.showShort(this, e.getMessage());
//			L.e("exception in moveRosterItemToGroup(): " + e.getMessage());
//		}
	}

	/**
	 * 重命名联系人
	 * @param user
	 * @param newName
	 */
	public void renameRosterItem(String user, String newName) {
//		try {
//			xChatDao.renameRosterItem(user, newName);
//		} catch (XChatException e) {
//			T.showShort(this, e.getMessage());
//			L.e("exception in renameRosterItem(): " + e.getMessage());
//		}
	}

	/**
	 * 重命名组
	 * @param group
	 * @param newGroup
	 */
	public void renameRosterGroup(String group, String newGroup) {
//		xChatDao.renameRosterGroup(group, newGroup);
	}

	/**
	 *  设置连接状态
	 */
	public void setStatusFromConfig() {
//		xChatDao.setStatusFromConfig();
	}

	/**
	 * 发送消息
	 * @param user
	 * @param message
	 */
	public void sendMessage(String user, String message) {
		if (xChatDao != null){
			xChatDao.sendMessage(user, message);
		} else{
			//连接已断开，消息存入库表，登陆后发送。
			ContentValues values = new ContentValues();
			values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
			values.put(ChatConstants.JID, user);
			values.put(ChatConstants.MESSAGE, message);
			values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
			values.put(ChatConstants.DATE, System.currentTimeMillis());
			getContentResolver().insert(ChatProvider.CONTENT_URI, values);
		}
	}
	/**
	 * 发送文件
	 * @param user
	 * @param filePaht
	 */
	public void sendFile(String user, String filePaht) {
//		if (xChatDao != null){
//			xChatDao.sendFile(user, filePaht);
//		}
	}
	
	public class XChatBinder extends Binder {
		public XChatService getService() {
			return XChatService.this;
		}
	}

	/**
	 * 自动重连广播
	 * @author liang
	 */
	private class ReconnectAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			if (!PreferenceUtil.getPrefBoolean(PreferenceUtil.SETTING_AUTO_RECONNECT, true)) {
				return;
			}
			if (mConnectedState != DISCONNECTED) {
				return;
			}
			String account = PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, "");
			String password = PreferenceUtil.getPrefString(PreferenceUtil.PASSWORD, "");
			if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
				return;
			}
			login(account, password);
		}
	}

}
