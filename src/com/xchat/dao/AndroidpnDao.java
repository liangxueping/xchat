package com.xchat.dao;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import com.xchat.base.BaseDao;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.service.XChatService;

public class AndroidpnDao extends BaseDao implements IXChatDao {
	
	public AndroidpnDao(XChatService service) {
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
//			mXMPPConnection.sendPacket(newMessage);
		} else {
			addChatMessageToDB(ChatConstants.OUTGOING, account, message, ChatConstants.DS_NEW, System.currentTimeMillis(), newMessage.getPacketID());
		}
	}
}
