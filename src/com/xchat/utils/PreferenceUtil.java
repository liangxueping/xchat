package com.xchat.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;

import com.xchat.base.BaseApp;

public class PreferenceUtil {

	//缓存文件名
	public static final String PREF_FILE_NAME = "common_settings";
	
	public static final String APP_VERSION= "app_version";

	public static final String ACCOUNT = "account";
	public static final String PASSWORD = "password";
	
	public static final String STATUS_MESSAGE = "status_message";
	/**
	 * 使用TLS加密
	 */
	public static final String SETTING_USE_TLS = "use_tls";
	/**
	 * 隐身登陆
	 */
	public static final String SETTING_SILENCE_LOGIN = "silence_login";
	/**
	 * 登录状态
	 */
	public static final String SETTING_STATUS_MODE = "status_mode";
	/**
	 * 震动
	 */
	public final static String SETTING_VIBRATION = "vibration";
	/**
	 * 显示新消息
	 */
	public final static String SETTING_SHOW_NEW_MESSAGE = "show_new_message";
	/**
	 * 显示新消息
	 */
	public final static String SETTING_SHOW_LED = "show_LED";
	/**
	 * 断线自动重连
	 */
	public final static String SETTING_AUTO_RECONNECT = "auto_reconnect";
	/**
	 * 开机自动接收消息
	 */
	public final static String SETTING_AUTO_RECEIVE_MESSAGE = "auto_receive_message";
	/**
	 * 通知栏关机图标
	 */
	public final static String SETTING_FOREGROUND = "foreground_service";
	/**
	 * 显示离线好友
	 */
	public final static String SETTING_SHOW_OFFLINE = "show_offline";
	
	public static final String OFFLINE = "offline";
	public static final String DND = "dnd";
	public static final String XA = "xa";
	public static final String AWAY = "away";
	public static final String AVAILABLE = "available";
	public static final String CHAT = "chat";
	
	
	public static boolean hasKey(final String key) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.contains(key);
	}
	
	public static String getPrefString(String key, final String defaultValue) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getString(key, defaultValue);
	}

	public static void setPrefString(final String key, final String value) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putString(key, value).commit();
	}

	public static boolean getPrefBoolean(final String key, final boolean defaultValue) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getBoolean(key, defaultValue);
	}

	public static void setPrefBoolean(final String key, final boolean value) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putBoolean(key, value).commit();
	}

	public static void setPrefInt(final String key, final int value) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putInt(key, value).commit();
	}

	public static int getPrefInt(final String key, final int defaultValue) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getInt(key, defaultValue);
	}

	public static void setPrefFloat(final String key, final float value) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putFloat(key, value).commit();
	}

	public static float getPrefFloat(final String key, final float defaultValue) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getFloat(key, defaultValue);
	}

	public static void setSettingLong(final String key, final long value) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putLong(key, value).commit();
	}

	public static long getPrefLong(final String key, final long defaultValue) {
		SharedPreferences settings = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getLong(key, defaultValue);
	}

	public static void remove(String keyWord) {
		SharedPreferences sp = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME,PreferenceActivity.MODE_PRIVATE);
		sp.edit().remove(keyWord).commit();
	}
	public static void clear() {
		SharedPreferences sp = BaseApp.getInstance().getSharedPreferences(PREF_FILE_NAME,PreferenceActivity.MODE_PRIVATE);
		sp.edit().clear().commit();
	}
}
