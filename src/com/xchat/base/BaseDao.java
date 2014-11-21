package com.xchat.base;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import com.xchat.db.ChatProvider;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.db.RosterProvider;
import com.xchat.db.RosterProvider.RosterConstants;
import com.xchat.utils.StatusMode;

public class BaseDao {

	/**
	 * 未连接
	 */
	public static boolean IS_DEBUG = false;
	
	public ContentResolver mContentResolver;
	
	public BaseDao(){
		IS_DEBUG = true;
		if(IS_DEBUG){
			mContentResolver = BaseApp.getInstance().getContentResolver();
			initDebugData();
		}
	}
	/**
	 * 消息记录存入数据库
	 * @param direction 消息类型
	 * @param account 用户名
	 * @param message 消息内容
	 * @param delivery_status 消息状态
	 * @param ts 创建时间
	 * @param packetID
	 */
	public void addChatMessageToDB(int direction, String account, String message, int delivery_status, long ts, String packetID) {
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, account);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);

		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
	}
	/**
	 * 更新离线状态
	 */
	public void setStatusOffline() {
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}
	/**
	 * 修改信息发送状态
	 * @param packetID
	 * @param new_status
	 */
	public void changeMessageDeliveryStatus(String packetID, int new_status) {
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + ChatProvider.TABLE_NAME);
		String where = ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING;
		mContentResolver.update(rowuri, cv, where, new String[] { packetID });
	}
	/**
	 * 初始化测试数据
	 */
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
