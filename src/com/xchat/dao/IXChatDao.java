package com.xchat.dao;


public interface IXChatDao {
	public boolean login(String account, String password);
	
	public boolean logout();

	public boolean isAuthenticated();

}
