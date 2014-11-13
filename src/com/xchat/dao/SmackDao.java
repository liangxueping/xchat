package com.xchat.dao;

import android.content.ContentResolver;
import android.content.ContentValues;

import com.xchat.db.RosterProvider;
import com.xchat.db.RosterProvider.RosterConstants;
import com.xchat.service.XChatService;

public class SmackDao implements IXChatDao {

	/**
	 * 未连接
	 */
	public static boolean IS_DEBUG = false;
	
	private final ContentResolver mContentResolver;
	
	public SmackDao(XChatService service) {
		IS_DEBUG = true;
		
		mContentResolver = service.getContentResolver();
		
		if(IS_DEBUG){
			initDebugData();
		}
	}

	@Override
	public boolean login(String account, String password){
		if(IS_DEBUG){
			return true;
		}
		
		return false;
	}

	@Override
	public boolean isAuthenticated() {
		if(IS_DEBUG){
			return true;
		}
		
		return false;
	}

	@Override
	public boolean logout() {
		if(IS_DEBUG){
			return true;
		}
		
		return false;
	}

	public void initDebugData() {
		mContentResolver.delete(RosterProvider.CONTENT_URI, null, null);
		int i = 0;
		for (; i < 10; i++){
			final ContentValues values = new ContentValues();
			values.put(RosterConstants.JID, Math.random()*10000);
			values.put(RosterConstants.ALIAS, "Name:"+i);
			values.put(RosterConstants.STATUS_MODE, 1);
			values.put(RosterConstants.STATUS_MESSAGE, 1);
			values.put(RosterConstants.GROUP, "我的好友");
			mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		}
		for (; i < 20; i++){
			final ContentValues values = new ContentValues();
			values.put(RosterConstants.JID, Math.random()*10000);
			values.put(RosterConstants.ALIAS, "Name:"+i);
			values.put(RosterConstants.STATUS_MODE, 1);
			values.put(RosterConstants.STATUS_MESSAGE, 1);
			values.put(RosterConstants.GROUP, "同事");
			mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		}
	}
}
