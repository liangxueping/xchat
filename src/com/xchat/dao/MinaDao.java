package com.xchat.dao;

import android.net.NetworkInfo;

import com.ksu.xchatandroidcore.OnXChatMessageListener;
import com.ksu.xchatandroidcore.XChatListenerManager;
import com.ksu.xchatandroidcore.XChatPushManager;
import com.ksu.xchatcore.nio.common.RequestKey;
import com.ksu.xchatcore.nio.model.Message;
import com.ksu.xchatcore.nio.model.ReplyBody;
import com.ksu.xchatcore.nio.model.SentBody;
import com.xchat.base.BaseDao;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.service.XChatService;

public class MinaDao extends BaseDao implements IXChatDao, OnXChatMessageListener{

	private XChatService mService;
	public MinaDao(XChatService service) {
		mService = service;
		mContentResolver = service.getContentResolver();
		XChatListenerManager.registerMessageListener(this, service);
	}
	
	@Override
	public boolean login(String account, String password) {
		XChatPushManager.setAccount(mService, account);
		return true;
	}

	@Override
	public boolean logout() {
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
	public void sendMessage(String account, String message) {
		SentBody sent = new SentBody();
		sent.setKey(RequestKey.CLIENT_MESSAGE.getValue());
		sent.put("sender", "liangxp");
		sent.put("receiver", "liangxp");
		sent.put("content", message);
		sent.put("type", "0");
		XChatPushManager.sendRequest(mService, sent);
		addChatMessageToDB(ChatConstants.OUTGOING, account, message, ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(), "");
	}

	@Override
	public void sendFile(String account, String filePaht) {
		SentBody sent = new SentBody();
		sent.setKey(RequestKey.CLIENT_BIND.getValue());
		sent.put("sender", "liangxueping");
		sent.put("receiver", "liangxp");
		sent.put("content", filePaht);
		sent.put("type", "0");
		XChatPushManager.sendRequest(mService, sent);
		addChatMessageToDB(ChatConstants.OUTGOING, account, filePaht, ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(), "");
	}

	@Override
	public void setStatusFromConfig() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getNameByID(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addRosterItem(String user, String alias, String group) {
		if(IS_DEBUG){
			return true;
		}
		return false;
	}

	@Override
	public boolean removeRosterItem(String account) {
		if(IS_DEBUG){
			return true;
		}
		return false;
	}

	@Override
	public boolean moveRosterItemToGroup(String user, String group) {
		if(IS_DEBUG){
			return true;
		}
		return false;
	}

	@Override
	public boolean renameRosterItem(String user, String newName) {
		if(IS_DEBUG){
			return true;
		}
		return false;
	}

	@Override
	public boolean renameRosterGroup(String group, String newGroup) {
		if(IS_DEBUG){
			return true;
		}
		return false;
	}

	@Override
	public void onConnectionClosed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionStatus(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionSucceed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceived(Message message) {
		
		addChatMessageToDB(ChatConstants.INCOMING, message.getSender(), message.getContent(), ChatConstants.DS_NEW, System.currentTimeMillis(), "");
		mService.newMessage(message.getSender(), message.getContent());
	}

	@Override
	public void onNetworkChanged(NetworkInfo arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReplyReceived(ReplyBody arg0) {
		// TODO Auto-generated method stub
		
	}

}
