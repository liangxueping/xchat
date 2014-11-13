package com.xchat.dao;

import com.xchat.service.XChatService;

public class SmackDao implements IXChatDao {

	/**
	 * 未连接
	 */
	public static boolean IS_DEBUG = false;
	
	public SmackDao(XChatService service) {
		IS_DEBUG = true;
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

}
