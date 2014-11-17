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
	/**
	 * 发送文件
	 * @param user
	 * @param filePaht
	 */
	public void sendFile(String user, String filePaht);
	/**
	 * 更新在线状态
	 */
	public void setStatusFromConfig();
	/**
	 * 根据ID获取显示名称
	 * @param id
	 * @return
	 */
	public String getNameByID(String id);
	/**
	 * 新增联系人
	 * @param user
	 * @param alias
	 * @param group
	 */
	public boolean addRosterItem(String user, String alias, String group);
	/**
	 * 删除联系人
	 * @param account
	 * @return
	 */
	public boolean removeRosterItem(String account);
	/**
	 * 将联系人移动到其他组
	 * @param user
	 * @param group
	 */
	public boolean moveRosterItemToGroup(String user, String group);

	/**
	 * 重命名联系人
	 * @param user
	 * @param newName
	 */
	public boolean renameRosterItem(String user, String newName);
	/**
	 * 重命名组
	 * @param group
	 * @param newGroup
	 */
	public boolean renameRosterGroup(String group, String newGroup);
}
