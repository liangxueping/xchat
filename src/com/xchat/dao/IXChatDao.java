package com.xchat.dao;



public interface IXChatDao {
	/**
	 * 登录
	 * @param account 用户名
	 * @param password 密码
	 * @return
	 */
	public boolean login(String account, String password);
	/**
	 * 退出
	 * @return
	 */
	public boolean logout();
	/**
	 * 验证连接是否存在
	 * @return
	 */
	public boolean isAuthenticated();
	/**
	 * 发送消息（文字）
	 * @param account
	 * @param message
	 */
	public void sendMessage(String account, String message);
	

}
