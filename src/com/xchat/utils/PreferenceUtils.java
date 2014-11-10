package com.xchat.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;

import com.xchat.base.XChatApp;

public class PreferenceUtils {

	//缓存文件名
	public static final String PREF_FILE_NAME = "common_settings";

	public static final String PASSWORD = "password";
	
	
	
	public static boolean hasKey(final String key) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.contains(key);
	}
	
	public static String getPrefString(String key, final String defaultValue) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getString(key, defaultValue);
	}

	public static void setPrefString(final String key, final String value) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putString(key, value).commit();
	}

	public static boolean getPrefBoolean(final String key, final boolean defaultValue) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getBoolean(key, defaultValue);
	}

	public static void setPrefBoolean(final String key, final boolean value) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putBoolean(key, value).commit();
	}

	public static void setPrefInt(final String key, final int value) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putInt(key, value).commit();
	}

	public static int getPrefInt(final String key, final int defaultValue) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getInt(key, defaultValue);
	}

	public static void setPrefFloat(final String key, final float value) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putFloat(key, value).commit();
	}

	public static float getPrefFloat(final String key, final float defaultValue) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getFloat(key, defaultValue);
	}

	public static void setSettingLong(final String key, final long value) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		settings.edit().putLong(key, value).commit();
	}

	public static long getPrefLong(final String key, final long defaultValue) {
		SharedPreferences settings = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME, PreferenceActivity.MODE_PRIVATE);
		return settings.getLong(key, defaultValue);
	}

	public static void remove(String keyWord) {
		SharedPreferences sp = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME,PreferenceActivity.MODE_PRIVATE);
		sp.edit().remove(keyWord).commit();
	}
	public static void clear() {
		SharedPreferences sp = XChatApp.getInstance().getSharedPreferences(PREF_FILE_NAME,PreferenceActivity.MODE_PRIVATE);
		sp.edit().clear().commit();
	}
}
