package com.xchat.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.xchat.activity.LoginActivity;
import com.xchat.activity.R;
import com.xchat.service.XChatService;
import com.xchat.utils.DialogUtil;
import com.xchat.utils.PreferenceUtil;
import com.xchat.view.CustomDialog;
import com.xchat.view.SlideSwitch;

public class SettingsFragment extends Fragment implements OnClickListener,
		com.xchat.view.SlideSwitch.OnCheckedChangeListener {
	private TextView mTitleNameView;
	private View mAccountSettingView;
	private ImageView mHeadIcon;
	private ImageView mStatusIcon;
	private TextView mStatusView;
	private TextView mNickView;
	private SlideSwitch mShowOfflineRosterSwitch;
	private SlideSwitch mNotifyRunBackgroundSwitch;
	private SlideSwitch mNewMsgSoundSwitch;
	private SlideSwitch mNewMsgVibratorSwitch;
	private SlideSwitch mNewMsgLedSwitch;
	private SlideSwitch mVisiableNewMsgSwitch;
	private SlideSwitch mShowHeadSwitch;
	private SlideSwitch mConnectionAutoSwitch;
	private SlideSwitch mPoweronReceiverMsgSwitch;
	private SlideSwitch mSendCrashSwitch;
	private View mFeedBackView;
	private View mAboutView;
	private Button mExitBtn;
	private View mExitMenuView;
	private Button mExitCancleBtn;
	private Button mExitConfirmBtn;
	private IFragmentCallBack mFragmentCallBack;
	private Context mContext;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mFragmentCallBack = (IFragmentCallBack) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnHeadlineSelectedListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_settings_fragment, container, false);
		mContext = view.getContext();
		return view;
	}
	
	public Context getContext(){
		return mContext;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mExitMenuView = LayoutInflater.from(getActivity()).inflate(R.layout.common_menu_dialog_2btn_layout, null);
		mExitCancleBtn = (Button) mExitMenuView.findViewById(R.id.btnCancel);
		mExitConfirmBtn = (Button) mExitMenuView.findViewById(R.id.btn_exit_comfirm);
		mExitConfirmBtn.setText(R.string.exit);
		mExitCancleBtn.setOnClickListener(this);
		mExitConfirmBtn.setOnClickListener(this);
		mTitleNameView = (TextView) view.findViewById(R.id.ivTitleName);
		mTitleNameView.setText(R.string.settings_fragment_title);
		mAccountSettingView = view.findViewById(R.id.accountSetting);
		mAccountSettingView.setOnClickListener(this);
		mHeadIcon = (ImageView) view.findViewById(R.id.face);
		mStatusIcon = (ImageView) view.findViewById(R.id.statusIcon);
		mStatusView = (TextView) view.findViewById(R.id.status);
		mNickView = (TextView) view.findViewById(R.id.nick);
		mShowOfflineRosterSwitch = (SlideSwitch) view.findViewById(R.id.show_offline_roster_switch);
		mShowOfflineRosterSwitch.setOnCheckedChangeListener(this);
		mNotifyRunBackgroundSwitch = (SlideSwitch) view.findViewById(R.id.notify_run_background_switch);
		mNotifyRunBackgroundSwitch.setOnCheckedChangeListener(this);
		mNewMsgSoundSwitch = (SlideSwitch) view.findViewById(R.id.new_msg_sound_switch);
		mNewMsgSoundSwitch.setOnCheckedChangeListener(this);
		mNewMsgVibratorSwitch = (SlideSwitch) view.findViewById(R.id.new_msg_vibrator_switch);
		mNewMsgVibratorSwitch.setOnCheckedChangeListener(this);
		mNewMsgLedSwitch = (SlideSwitch) view.findViewById(R.id.new_msg_led_switch);
		mNewMsgLedSwitch.setOnCheckedChangeListener(this);
		mVisiableNewMsgSwitch = (SlideSwitch) view.findViewById(R.id.visiable_new_msg_switch);
		mVisiableNewMsgSwitch.setOnCheckedChangeListener(this);
		mShowHeadSwitch = (SlideSwitch) view.findViewById(R.id.show_head_switch);
		mShowHeadSwitch.setOnCheckedChangeListener(this);
		mConnectionAutoSwitch = (SlideSwitch) view.findViewById(R.id.connection_auto_switch);
		mConnectionAutoSwitch.setOnCheckedChangeListener(this);
		mPoweronReceiverMsgSwitch = (SlideSwitch) view.findViewById(R.id.poweron_receiver_msg_switch);
		mPoweronReceiverMsgSwitch.setOnCheckedChangeListener(this);
		mSendCrashSwitch = (SlideSwitch) view.findViewById(R.id.send_crash_switch);
		mSendCrashSwitch.setOnCheckedChangeListener(this);
		mFeedBackView = view.findViewById(R.id.set_feedback);
		mAboutView = view.findViewById(R.id.set_about);
		mExitBtn = (Button) view.findViewById(R.id.exit_app);
		mFeedBackView.setOnClickListener(this);
		mAboutView.setOnClickListener(this);
		mExitBtn.setOnClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		readData();
	}

	public void readData() {
		mHeadIcon.setImageResource(R.drawable.login_default_avatar);
		mStatusIcon.setImageResource(FriendsFragment.mStatusMap.get(
				PreferenceUtil.getPrefString(
						PreferenceUtil.SETTING_STATUS_MODE,
						PreferenceUtil.AVAILABLE)));
		mStatusView.setText(PreferenceUtil.getPrefString(
				PreferenceUtil.STATUS_MESSAGE,
				getActivity().getString(R.string.status_available)));
		mNickView.setText(PreferenceUtil.getPrefString(PreferenceUtil.ACCOUNT, ""));
		mShowOfflineRosterSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_SHOW_OFFLINE, true));

		mNotifyRunBackgroundSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_FOREGROUND, true));
		mNewMsgSoundSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_SCLIENT_NOTIFY, false));
		mNewMsgVibratorSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_VIBRATION, true));
		mNewMsgLedSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_SHOW_LED, true));
		mVisiableNewMsgSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_SHOW_NEW_MESSAGE, true));
		mShowHeadSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_SHOW_MY_HEAD, true));
		mConnectionAutoSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_AUTO_RECONNECT, true));
		mPoweronReceiverMsgSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_AUTO_RECEIVE_MESSAGE, true));
		mSendCrashSwitch.setChecked(PreferenceUtil.getPrefBoolean(
				 PreferenceUtil.SETTING_REPORT_CRASH, true));
	}

	private Dialog mExitDialog;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.set_feedback:
//			startActivity(new Intent(getActivity(), FeedBackActivity.class));
			break;
		case R.id.set_about:
//			startActivity(new Intent(getActivity(), AboutActivity.class));
			break;
		case R.id.exit_app:
			if (mExitDialog == null)
				mExitDialog = DialogUtil.getMenuDialog(getActivity(),
						mExitMenuView);
			mExitDialog.show();
			break;
		case R.id.btnCancel:
			if (mExitDialog != null && mExitDialog.isShowing())
				mExitDialog.dismiss();
			break;
		case R.id.btn_exit_comfirm:
			XChatService service = mFragmentCallBack.getService();
			if (service != null) {
				service.logout();// 注销
				service.stopSelf();// 停止服务
			}
			if(mExitDialog.isShowing()){
				mExitDialog.cancel();
			}
			getActivity().finish();
			break;
		case R.id.accountSetting:
			logoutDialog();
			break;
		default:
			break;
		}
	}

	public void logoutDialog() {
		new CustomDialog.Builder(getActivity())
				.setTitle(getActivity().getString(R.string.open_switch_account))
				.setMessage(
						getActivity().getString(
								R.string.open_switch_account_msg))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								XChatService service = mFragmentCallBack
										.getService();
								if (service != null) {
									service.logout();// 注销
								}
								dialog.dismiss();
								startActivity(new Intent(getActivity(), LoginActivity.class));
								getActivity().finish();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						}).create().show();
	}

	@Override
	public void onCheckedChanged(View v, boolean isChecked) {
		switch (v.getId()) {
		case R.id.show_offline_roster_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_SHOW_OFFLINE, isChecked);
			mFragmentCallBack.getMainActivity().updateRoster();
			break;
		case R.id.notify_run_background_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_FOREGROUND, isChecked);
			break;
		case R.id.new_msg_sound_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_SCLIENT_NOTIFY, isChecked);
			break;
		case R.id.new_msg_vibrator_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_VIBRATION, isChecked);
			break;
		case R.id.new_msg_led_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_SHOW_LED, isChecked);
			break;
		case R.id.visiable_new_msg_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_SHOW_NEW_MESSAGE, isChecked);
			break;
		case R.id.show_head_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_SHOW_MY_HEAD, isChecked);
			break;
		case R.id.connection_auto_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_AUTO_RECONNECT, isChecked);
			break;
		case R.id.poweron_receiver_msg_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_AUTO_RECEIVE_MESSAGE, isChecked);
			break;
		case R.id.send_crash_switch:
			PreferenceUtil.setPrefBoolean(
					PreferenceUtil.SETTING_REPORT_CRASH, isChecked);

			break;
		default:
			break;
		}
	}
}
