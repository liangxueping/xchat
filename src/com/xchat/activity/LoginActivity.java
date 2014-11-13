package com.xchat.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.xchat.service.IConnectionStatusCallback;
import com.xchat.service.XChatService;
import com.xchat.system.T;
import com.xchat.utils.DialogUtil;
import com.xchat.utils.MyUtil;
import com.xchat.utils.PreferenceUtil;

@SuppressLint("HandlerLeak")
public class LoginActivity extends FragmentActivity implements TextWatcher,IConnectionStatusCallback{

	public static final String LOGIN_ACTION = "com.xchat.action.LOGIN";
	
	private XChatService xChatService;
	private ConnectionOutTimeProcess mLoginOutTimeProcess;
	private Dialog dialogLogin;
	
	private Animation animAlpha;
	private EditText etAccount;
	private EditText etPassword;
	private Button btnLogin;
	private CheckBox cbAutoSavePassword;
	private CheckBox cbHideLogin;
	private CheckBox cbUseTls;
	private CheckBox cbSilenceLogin;
	
	private View mTipsViewRoot;
	private TextView tvCloseTips;
	
	private String mAccount;
	private String mPassword;

	private static final int LOGIN_OUT_TIME = 0;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case LOGIN_OUT_TIME:
				if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running)
					mLoginOutTimeProcess.stop();
				if (dialogLogin != null && dialogLogin.isShowing())
					dialogLogin.dismiss();
				T.showShort(LoginActivity.this, R.string.timeout_try_again);
				break;

			default:
				break;
			}
		}

	};

	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xChatService = ((XChatService.XChatBinder) service).getService();
			xChatService.registerConnectionStatusCallback(LoginActivity.this);
			// 开始连接xmpp服务器
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			xChatService.unRegisterConnectionStatusCallback();
			xChatService = null;
		}

	};

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	private void bindXMPPService() {
		Intent mServiceIntent = new Intent(this, XChatService.class);
		mServiceIntent.setAction(LOGIN_ACTION);
		bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		initView();
		bindXMPPService();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(TextUtils.equals(PreferenceUtil.getPrefString(PreferenceUtil.APP_VERSION, ""), getString(R.string.app_version)) 
				&& !TextUtils.isEmpty(PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, ""))) {
			mTipsViewRoot.setVisibility(View.GONE);
		} else {
			mTipsViewRoot.setVisibility(View.VISIBLE);
			PreferenceUtil.setPrefString(PreferenceUtil.APP_VERSION, getString(R.string.app_version));
		}
		
		if(tvCloseTips != null && animAlpha != null){
			tvCloseTips.startAnimation(animAlpha);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(tvCloseTips != null && animAlpha != null){
			tvCloseTips.clearAnimation();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mLoginOutTimeProcess != null) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
		unbindXMPPService();
	}
	
	private void initView() {
		animAlpha = AnimationUtils.loadAnimation(this, R.anim.connection);
		cbAutoSavePassword = (CheckBox) findViewById(R.id.cb_auto_save_password);
		cbHideLogin = (CheckBox) findViewById(R.id.cb_hide_login);
		cbSilenceLogin = (CheckBox) findViewById(R.id.cb_silence_login);
		cbUseTls = (CheckBox) findViewById(R.id.cb_use_tls);
		mTipsViewRoot = findViewById(R.id.login_help_view);
		tvCloseTips = (TextView) findViewById(R.id.tv_close_tips);
		etAccount = (EditText) findViewById(R.id.account_input);
		etPassword = (EditText) findViewById(R.id.password);
		btnLogin = (Button) findViewById(R.id.login);
		
		String account = PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, "");
		String password = PreferenceUtil.getPrefString(PreferenceUtil.PASSWORD, "");
		if(!TextUtils.isEmpty(account)){
			etAccount.setText(account);
		}
		if(!TextUtils.isEmpty(password)){
			etPassword.setText(password);
		}

		boolean isSilenceLogin = PreferenceUtil.getPrefBoolean(PreferenceUtil.SETTING_SILENCE_LOGIN, false);
		cbSilenceLogin.setChecked(isSilenceLogin);
		
		etAccount.addTextChangedListener(this);
		dialogLogin = DialogUtil.getLoginDialog(this);
		mLoginOutTimeProcess = new ConnectionOutTimeProcess();
	}

	public void onLoginClick(View v) {
		mAccount = etAccount.getText().toString();
		mPassword = etPassword.getText().toString();
		if(TextUtils.isEmpty(mAccount)) {
			T.showShort(this, R.string.null_account);
			return;
		}
		if(TextUtils.isEmpty(mPassword)) {
			T.showShort(this, R.string.null_password);
			return;
		}
		if(mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running){
			mLoginOutTimeProcess.start();
		}
		if(dialogLogin != null && !dialogLogin.isShowing()){
			dialogLogin.show();
		}
		if(xChatService != null) {
			xChatService.login(mAccount, mPassword);
		}
		
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		if(MyUtil.validateAccount(s.toString())){
			btnLogin.setEnabled(true);
			etAccount.setTextColor(Color.parseColor("#ff333333"));
		}else {
			btnLogin.setEnabled(false);
			etAccount.setTextColor(Color.RED);
		}
	}

	private void save2Preferences() {
		boolean isAutoSavePassword = cbAutoSavePassword.isChecked();
		boolean isUseTls = cbUseTls.isChecked();
		boolean isSilenceLogin = cbSilenceLogin.isChecked();
		boolean isHideLogin = cbHideLogin.isChecked();
		//帐号是一直保存的
		PreferenceUtil.setPrefString(PreferenceUtil.ACCOUNT, mAccount);
		if(isAutoSavePassword){
			PreferenceUtil.setPrefString(PreferenceUtil.PASSWORD, mPassword);
		} else{
			PreferenceUtil.setPrefString(PreferenceUtil.PASSWORD, "");
		}
		PreferenceUtil.setPrefBoolean(PreferenceUtil.SETTING_USE_TLS, isUseTls);
		PreferenceUtil.setPrefBoolean(PreferenceUtil.SETTING_SILENCE_LOGIN, isSilenceLogin);
		if(isHideLogin){
			PreferenceUtil.setPrefString(PreferenceUtil.SETTING_STATUS, PreferenceUtil.XA);
		} else{
			PreferenceUtil.setPrefString(PreferenceUtil.SETTING_STATUS, PreferenceUtil.AVAILABLE);
		}
	}

	// 登录超时处理线程
	class ConnectionOutTimeProcess implements Runnable {
		public boolean running = false;
		private long startTime = 0L;
		private Thread thread = null;

		public void run() {
			while (true) {
				if (!this.running){
					return;
				}
				if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
					mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
				}
				try {
					Thread.sleep(10L);
				} catch (Exception localException) {
					
				}
			}
		}

		public void start() {
			this.thread = new Thread(this);
			this.running = true;
			this.startTime = System.currentTimeMillis();
			this.thread.start();
		}

		public void stop() {
			this.running = false;
			this.thread = null;
			this.startTime = 0L;
		}
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		if (dialogLogin != null && dialogLogin.isShowing()){
			dialogLogin.dismiss();
		}
		if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
		if (connectedState == XChatService.CONNECTED) {
			save2Preferences();
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} else if (connectedState == XChatService.DISCONNECTED){
			T.showLong(LoginActivity.this, getString(R.string.tip_login_failed) + reason);
		}
	}
}
