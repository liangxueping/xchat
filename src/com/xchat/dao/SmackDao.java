package com.xchat.dao;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import com.xchat.base.BaseDao;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.service.XChatService;
import com.xchat.utils.PreferenceUtil;

public class SmackDao extends BaseDao implements IXChatDao {
	

	private XChatService xChartService;
	private ConnectionConfiguration mXMPPConfig;
	private XMPPConnection mXMPPConnection;
	
	
	public SmackDao(XChatService service) {
		IS_DEBUG = true;
		this.xChartService = service;
		mContentResolver = service.getContentResolver();
		
		boolean requireSsl = PreferenceUtil.getPrefBoolean(PreferenceUtil.SETTING_USE_TLS, false);
		
		if(IS_DEBUG){
			initDebugData();
		}else {
			this.mXMPPConfig = new ConnectionConfiguration(PreferenceUtil.HOST_SERVER, PreferenceUtil.HOST_PORT);
			this.mXMPPConfig.setReconnectionAllowed(false);
			this.mXMPPConfig.setSendPresence(false);
			this.mXMPPConfig.setCompressionEnabled(false); // disable for now
			this.mXMPPConfig.setDebuggerEnabled(false);
			if (requireSsl){
				this.mXMPPConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
			}
			this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
			mContentResolver = service.getContentResolver();
			
			Roster.setDefaultSubscriptionMode(SubscriptionMode.accept_all);
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
		final Message newMessage = new Message(account, Message.Type.chat);
		newMessage.setBody(message);
		newMessage.addExtension(new DeliveryReceiptRequest());
		if (isAuthenticated()) {
			addChatMessageToDB(ChatConstants.OUTGOING, account, message, ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(), newMessage.getPacketID());
			if(IS_DEBUG){
				mXMPPConnection.sendPacket(newMessage);
			}
		} else {
			addChatMessageToDB(ChatConstants.OUTGOING, account, message, ChatConstants.DS_NEW, System.currentTimeMillis(), newMessage.getPacketID());
		}
	}
}
