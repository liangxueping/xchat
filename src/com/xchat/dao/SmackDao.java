package com.xchat.dao;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.xchat.activity.R;
import com.xchat.base.BaseDao;
import com.xchat.db.ChatProvider;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.db.RosterProvider;
import com.xchat.db.RosterProvider.RosterConstants;
import com.xchat.service.XChatService;
import com.xchat.system.L;
import com.xchat.utils.PreferenceUtil;
import com.xchat.utils.StatusMode;

public class SmackDao extends BaseDao implements IXChatDao {

	private static final int PACKET_TIMEOUT = 30000;
	private static final String PING_ALARM = "com.xchat.PING_ALARM";
	private static final String PONG_TIMEOUT_ALARM = "com.xchat.PONG_TIMEOUT_ALARM";
	private final static String[] SEND_OFFLINE_PROJECTION = new String[] {
		ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE,
		ChatConstants.DATE, ChatConstants.PACKET_ID };
	private final static String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION
		+ " = "
		+ ChatConstants.OUTGOING
		+ " AND "
		+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	private XChatService xChatService;
	private ConnectionConfiguration mXMPPConfig;
	private XMPPConnection mXMPPConnection;
	private Roster mRoster;
	private RosterListener mRosterListener;
	private PacketListener mPacketListener;
	private PacketListener mSendFailureListener;
	private PacketListener mPongListener;
	private PendingIntent mPingAlarmPendIntent;
	private PendingIntent mPongTimeoutAlarmPendIntent;
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();
	private String mPingID;
	private long mPingTimestamp;
	
	
	public SmackDao(XChatService service) {
		IS_DEBUG = false;
		this.xChatService = service;
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
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mXMPPConnection.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			registerRosterListener();//监听联系人动态变化
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected()) {
				return false;
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
			initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				mXMPPConnection.login(account, password, PreferenceUtil.HOST_RESSOURCE);
			}
			setStatusFromConfig();// 更新在线状态

		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		registerAllListener();// 注册监听其他的事件，比如新消息
		return mXMPPConnection.isAuthenticated();
	}

	@Override
	public boolean logout() {
		if(IS_DEBUG){
			return true;
		}
		try {
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection.removePacketSendFailureListener(mSendFailureListener);
			mXMPPConnection.removePacketListener(mPongListener);
			((AlarmManager) xChatService.getSystemService(Context.ALARM_SERVICE)).cancel(mPingAlarmPendIntent);
			((AlarmManager) xChatService.getSystemService(Context.ALARM_SERVICE)).cancel(mPongTimeoutAlarmPendIntent);
			xChatService.unregisterReceiver(mPingAlarmReceiver);
			xChatService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		} catch (Exception e) {
			// ignore it!
			return false;
		}
		if (mXMPPConnection.isConnected()) {
			// work around SMACK's #%&%# blocking disconnect()
			new Thread() {
				public void run() {
					mXMPPConnection.disconnect();
				}
			}.start();
		}
		setStatusOffline();
		this.xChatService = null;
		return true;
	}

	@Override
	public boolean isAuthenticated() {
		if(IS_DEBUG){
			return true;
		}else if (mXMPPConnection != null) {
			return (mXMPPConnection.isConnected() && mXMPPConnection.isAuthenticated());
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
			if(!IS_DEBUG){
				mXMPPConnection.sendPacket(newMessage);
			}
		} else {
			addChatMessageToDB(ChatConstants.OUTGOING, account, message, ChatConstants.DS_NEW, System.currentTimeMillis(), newMessage.getPacketID());
		}
	}
	@Override
	public String getNameByID(String id) {
		if (null != this.mRoster.getEntry(id)
				&& null != this.mRoster.getEntry(id).getName()
				&& this.mRoster.getEntry(id).getName().length() > 0) {
			return this.mRoster.getEntry(id).getName();
		} else {
			return id;
		}
	}

	private void sendServerPing() {
		if (mPingID != null) {
			return; // a ping is still on its way
		}
		Ping ping = new Ping();
		ping.setType(Type.GET);
		ping.setTo(PreferenceUtil.HOST_SERVER);
		mPingID = ping.getPacketID();
		mPingTimestamp = System.currentTimeMillis();
		mXMPPConnection.sendPacket(ping);

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		((AlarmManager) xChatService.getSystemService(Context.ALARM_SERVICE)).set(
			AlarmManager.RTC_WAKEUP, 
			System.currentTimeMillis() + PACKET_TIMEOUT + 3000, 
			mPongTimeoutAlarmPendIntent);
	}

	private void updateRosterEntryInDB(final RosterEntry entry) {
		final ContentValues values = getContentValuesForRosterEntry(entry);

		if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
				RosterConstants.JID + " = ?", new String[] { entry.getUser() }) == 0)
			addRosterEntryToDB(entry);
	}

	private void addRosterEntryToDB(final RosterEntry entry) {
		ContentValues values = getContentValuesForRosterEntry(entry);
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		L.i("addRosterEntryToDB: Inserted " + uri);
	}
	private void deleteRosterEntryFromDB(final String jabberID) {
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				RosterConstants.JID + " = ?", new String[] { jabberID });
		L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}
	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));
		Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));

		return values;
	}
	private String getGroup(Collection<RosterGroup> groups) {
		for (RosterGroup group : groups) {
			return group.getName();
		}
		return "";
	}
	private int getStatusInt(final Presence presence) {
		return getStatus(presence).ordinal();
	}
	private StatusMode getStatus(Presence presence) {
		if (presence.getType() == Presence.Type.available) {
			if (presence.getMode() != null) {
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.offline;
	}
	private String getName(RosterEntry rosterEntry) {
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0) {
			return name;
		}
		return rosterEntry.getUser();
	}
	/**
	 * 监听联系人变化
	 */
	private void registerRosterListener() {
		mRoster = mXMPPConnection.getRoster();
		mRosterListener = new RosterListener() {
			private boolean isFristRoter;

			@Override
			public void presenceChanged(Presence presence) {
				L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
				String jabberID = getJabberID(presence.getFrom());
				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntryInDB(rosterEntry);
				xChatService.rosterChanged();
			}

			@Override
			public void entriesUpdated(Collection<String> entries) {
				// TODO Auto-generated method stub
				L.i("entriesUpdated(" + entries + ")");
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				xChatService.rosterChanged();
			}

			@Override
			public void entriesDeleted(Collection<String> entries) {
				L.i("entriesDeleted(" + entries + ")");
				for (String entry : entries) {
					deleteRosterEntryFromDB(entry);
				}
				xChatService.rosterChanged();
			}

			@Override
			public void entriesAdded(Collection<String> entries) {
				L.i("entriesAdded(" + entries + ")");
				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				if (isFristRoter) {
					isFristRoter = false;
					xChatService.rosterChanged();
				}
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}

	/**
	 * 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
	 */
	private void initServiceDiscovery() {
		// register connection features
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection);
		if (sdm == null){
			sdm = new ServiceDiscoveryManager(mXMPPConnection);
		}
		sdm.addFeature("http://jabber.org/protocol/disco#info");
		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(10 * 1000);
		// reference DeliveryReceiptManager, add listener
		DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener() {
			public void onReceiptReceived(String fromJid, String toJid, String receiptId) {
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
			}
		});
	}
	/**
	 * 更新在线状态
	 */
	public void setStatusFromConfig() {
		String statusMode = PreferenceUtil.getPrefString(PreferenceUtil.SETTING_STATUS_MODE, PreferenceUtil.AVAILABLE);
		String statusMessage = PreferenceUtil.getPrefString(PreferenceUtil.STATUS_MESSAGE, xChatService.getString(R.string.status_online));
		
		CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(true);
		Presence presence = new Presence(Presence.Type.available);
		Mode mode = Mode.valueOf(statusMode);
		presence.setMode(mode);
		presence.setStatus(statusMessage);
		presence.setPriority(0);
		mXMPPConnection.sendPacket(presence);
	}
	/**
	 * 注册监听事件
	 */
	private void registerAllListener() {
		if (isAuthenticated()) {
			registerFileListener();
			registerMessageListener();
			registerMessageSendFailureListener();
			registerPongListener();
			sendOfflineMessages();
			if (xChatService == null) {
				mXMPPConnection.disconnect();
				return;
			}
			xChatService.rosterChanged();
		}
	}
	/**
	 * 监听文件接收
	 */
	private void registerFileListener() {
		FileTransferManager manager = new FileTransferManager(mXMPPConnection);
		manager.addFileTransferListener(new FileTransferListener() {
			@Override
			public void fileTransferRequest(FileTransferRequest request) {
				if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
	                IncomingFileTransfer accept = request.accept();
					String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	    			File file = new File(path + "/" + request.getFileName());
	    			try {
	    				accept.recieveFile(file);
	    				L.i("接收文件 path：" + file.getPath());
	    				String fromID = request.getRequestor();
						String user = getJabberID(fromID);

						addChatMessageToDB(ChatConstants.INCOMING, user, "接收文件："+file.getName(), ChatConstants.DS_NEW, System.currentTimeMillis(), "null");
						xChatService.newMessage(user, "接收文件："+file.getName());
	    			} catch (XMPPException e) {
	    				e.printStackTrace();
	    			}
				}else {
					request.reject();
    				L.i("拒绝文件");
				}
			}
		});
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
		mPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						// try to extract a carbon
						Carbon cc = CarbonManager.getCarbon(msg);
						if (cc != null && cc.getDirection() == Carbon.Direction.received) {
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							// fall through
						} else if (cc != null && cc.getDirection() == Carbon.Direction.sent) {
							L.d("carbon: " + cc.toXML());
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
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, orFilter);
	}
	/**
	 * 处理消息发送失败状态
	 */
	private void registerMessageSendFailureListener() {
		// do not register multiple packet listeners
		if (mSendFailureListener != null)
			mXMPPConnection.removePacketSendFailureListener(mSendFailureListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mSendFailureListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;
						changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_NEW);
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketSendFailureListener(mSendFailureListener, filter);
	}
	/**
	 * 处理ping服务器消息
	 */
	private void registerPongListener() {
		// reset ping expectation on new connection
		mPingID = null;

		if (mPongListener != null)
			mXMPPConnection.removePacketListener(mPongListener);

		mPongListener = new PacketListener() {

			@SuppressLint("DefaultLocale")
			@Override
			public void processPacket(Packet packet) {
				if (packet == null)
					return;

				if (packet.getPacketID().equals(mPingID)) {
					L.i(String.format(
							"Ping: server latency %1.3fs",
							(System.currentTimeMillis() - mPingTimestamp) / 1000.));
					mPingID = null;
					((AlarmManager) xChatService
							.getSystemService(Context.ALARM_SERVICE))
							.cancel(mPongTimeoutAlarmPendIntent);
				}
			}

		};

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(IQ.class));
		mPingAlarmPendIntent = PendingIntent.getBroadcast(xChatService.getApplicationContext(), 0, mPingAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(xChatService.getApplicationContext(), 0, mPongTimeoutAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		xChatService.registerReceiver(mPingAlarmReceiver, new IntentFilter(PING_ALARM));
		xChatService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(PONG_TIMEOUT_ALARM));
		((AlarmManager) xChatService.getSystemService(Context.ALARM_SERVICE))
				.setInexactRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						mPingAlarmPendIntent);
	}
	/**
	 * 发送离线消息
	 */
	public void sendOfflineMessages() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI, SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);
		if(cursor != null){

			final int _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
			final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
			final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
			final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
			final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
			ContentValues mark_sent = new ContentValues();
			mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
			while (cursor.moveToNext()) {
				int _id = cursor.getInt(_ID_COL);
				String toJID = cursor.getString(JID_COL);
				String message = cursor.getString(MSG_COL);
				String packetID = cursor.getString(PACKETID_COL);
				long ts = cursor.getLong(TS_COL);
				L.d("sendOfflineMessages: " + toJID + " > " + message);
				final Message newMessage = new Message(toJID, Message.Type.chat);
				newMessage.setBody(message);
				DelayInformation delay = new DelayInformation(new Date(ts));
				newMessage.addExtension(delay);
				newMessage.addExtension(new DelayInfo(delay));
				newMessage.addExtension(new DeliveryReceiptRequest());
				if ((packetID != null) && (packetID.length() > 0)) {
					newMessage.setPacketID(packetID);
				} else {
					packetID = newMessage.getPacketID();
					mark_sent.put(ChatConstants.PACKET_ID, packetID);
				}
				Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
						+ ChatProvider.TABLE_NAME + "/" + _id);
				mContentResolver.update(rowuri, mark_sent, null, null);
				mXMPPConnection.sendPacket(newMessage); // must be after marking
														// delivered, otherwise it
														// may override the
														// SendFailListener
			}
			cursor.close();
		}
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
	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			xChatService.postConnectionFailed(XChatService.PONG_TIMEOUT);
			logout();// 超时就断开连接
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			if (mXMPPConnection.isAuthenticated()) {
				sendServerPing();
			}
		}
	}
}
