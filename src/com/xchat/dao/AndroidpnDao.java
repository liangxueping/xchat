package com.xchat.dao;

import java.util.UUID;

import org.androidpn.client.Constants;
import org.androidpn.client.NotificationIQ;
import org.androidpn.client.NotificationIQProvider;
import org.androidpn.client.NotificationPacketListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.xchat.base.BaseDao;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.service.XChatService;
import com.xchat.system.T;
import com.xchat.utils.PreferenceUtil;

public class AndroidpnDao extends BaseDao implements IXChatDao {

	private XChatService xChatService;
	private ConnectionConfiguration mXMPPConfig;
	private XMPPConnection mXMPPConnection;
	private PacketListener mPacketListener;
	private String currentUsername;
	private String currentPassword;
	
	public AndroidpnDao(XChatService service) {

		xChatService = service;
		mContentResolver = service.getContentResolver();

		String hostServer = "10.200.16.46";
		// Create the configuration for this new connection
        mXMPPConfig = new ConnectionConfiguration(hostServer, PreferenceUtil.HOST_PORT);
        mXMPPConfig.setSecurityMode(SecurityMode.required);
        mXMPPConfig.setSASLAuthenticationEnabled(false);
        mXMPPConfig.setCompressionEnabled(false);

        mXMPPConnection = new XMPPConnection(mXMPPConfig);
		Roster.setDefaultSubscriptionMode(SubscriptionMode.accept_all);
		try {
			mXMPPConnection.connect();
			// packet provider
            ProviderManager.getInstance().addIQProvider("notification",
                    "androidpn:iq:notification",
                    new NotificationIQProvider());
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
		currentUsername = newRandomUUID();
        currentPassword = newRandomUUID();
        registered(currentUsername, currentPassword);
	}

    private String newRandomUUID() {
        String uuidRaw = UUID.randomUUID().toString();
        return uuidRaw.replaceAll("-", "");
    }
    
	private void registered(final String newUsername, final String newPassword){

        Registration registration = new Registration();

        PacketFilter packetFilter = new AndFilter(new PacketIDFilter(
                registration.getPacketID()), new PacketTypeFilter(
                IQ.class));

        PacketListener packetListener = new PacketListener() {

            public void processPacket(Packet packet) {
                if (packet instanceof IQ) {
                    IQ response = (IQ) packet;
                    if (response.getType() == IQ.Type.RESULT) {
                    	T.showLong(xChatService.getApplicationContext(), "用户注册成功！");
                    }
                }
            }
        };

        mXMPPConnection.addPacketListener(packetListener, packetFilter);

        registration.setType(IQ.Type.SET);
        registration.addAttribute("username", newUsername);
        registration.addAttribute("password", newPassword);
        mXMPPConnection.sendPacket(registration);
	}

	@Override
	public boolean login(String account, String password){
		if(IS_DEBUG){
			return true;
		}
		try {
			if (!mXMPPConnection.isAuthenticated()) {
				mXMPPConnection.login(currentUsername, currentPassword, PreferenceUtil.HOST_RESSOURCE);
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					xChatService.postConnectionFailed(e.getMessage());
				}

				public void connectionClosed() {
				}

				public void reconnectingIn(int seconds) {
				}

				public void reconnectionFailed(Exception e) {
				}

				public void reconnectionSuccessful() {
				}
			});
			 // packet filter
            PacketFilter packetFilter = new PacketTypeFilter(NotificationIQ.class);
            // packet listener
            PacketListener packetListener = new NotificationPacketListener(null);
            mXMPPConnection.addPacketListener(packetListener, packetFilter);
            registerMessageListener();
        } catch (XMPPException e) {
           e.printStackTrace();
   			return false;
        } catch (Exception e) {
            e.printStackTrace();
    		return false;
        }
		return true;
	}
	/**
	 * 监听消息（文字 ）接收
	 */
	private void registerMessageListener() {
		// do not register multiple packet listeners
		if (mPacketListener != null){
			mXMPPConnection.removePacketListener(mPacketListener);
		}

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);
		OrFilter orFilter = new OrFilter();
		orFilter.addFilter(filter);
        PacketFilter packetFilter = new PacketTypeFilter(NotificationIQ.class);
		orFilter.addFilter(packetFilter);
//		PacketListener packetListener = new NotificationPacketListener(null);
		mPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						// try to extract a carbon
						Carbon cc = CarbonManager.getCarbon(msg);
						if (cc != null && cc.getDirection() == Carbon.Direction.received) {
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							// fall through
						} else if (cc != null && cc.getDirection() == Carbon.Direction.sent) {
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							if (chatMessage == null)
								return;
							String fromJID = getJabberID(msg.getTo());

							addChatMessageToDB(ChatConstants.OUTGOING, fromJID, chatMessage, ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(), msg.getPacketID());
							// always return after adding
							return;
						}

						if (chatMessage == null) {
							return;
						}

						if (msg.getType() == Message.Type.error) {
							chatMessage = "<Error> " + chatMessage;
						}

						long ts;
						DelayInfo timestamp = (DelayInfo) msg.getExtension("delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo) msg.getExtension("x", "jabber:x:delay");
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();

						String fromJID = getJabberID(msg.getFrom());

						addChatMessageToDB(ChatConstants.INCOMING, fromJID, chatMessage, ChatConstants.DS_NEW, ts, msg.getPacketID());
						xChatService.newMessage(fromJID, chatMessage);
					}else if (packet instanceof NotificationIQ) {
			            NotificationIQ notification = (NotificationIQ) packet;

			            if (notification.getChildElementXML().contains(
			                    "androidpn:iq:notification")) {
			                String notificationId = notification.getId();
			                String notificationMessage = notification.getMessage();
			                long ts = System.currentTimeMillis();
			                addChatMessageToDB(ChatConstants.INCOMING, notificationId, notificationMessage, ChatConstants.DS_NEW, ts, "");
							xChatService.newMessage(notificationId, notificationMessage);
			            }
			        }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, orFilter);
	}
	/**
	 * 获取帐号
	 * @param from
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	private String getJabberID(String from) {
		if(from != null){
			return from.split("/")[0].toLowerCase();
		}
		return "";
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
	public void sendFile(String user, String filePaht) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean addRosterItem(String user, String alias, String group) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeRosterItem(String account) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean moveRosterItemToGroup(String user, String group) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean renameRosterItem(String user, String newName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean renameRosterGroup(String group, String newGroup) {
		// TODO Auto-generated method stub
		return false;
	}
}
