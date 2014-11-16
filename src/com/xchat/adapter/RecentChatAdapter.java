package com.xchat.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.xchat.activity.R;
import com.xchat.db.ChatProvider;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.utils.MyUtil;

public class RecentChatAdapter extends SimpleCursorAdapter {
	private static final String SELECT = ChatConstants.DATE
			+ " in (select max(" + ChatConstants.DATE + ") from "
			+ ChatProvider.TABLE_NAME + " group by " + ChatConstants.JID
			+ " having count(*)>0)";// 查询合并重复jid字段的所有聊天对象
	private static final String[] FROM = new String[] {
			ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION,
			ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
			ChatProvider.ChatConstants.DELIVERY_STATUS };// 查询字段
	private static final String SORT_ORDER = ChatConstants.DATE + " DESC";
	private ContentResolver mContentResolver;
	private LayoutInflater mLayoutInflater;
	private Activity mContext;

	@SuppressWarnings("deprecation")
	public RecentChatAdapter(Activity context) {
		super(context, 0, null, FROM, null);
		mContext = context;
		mContentResolver = context.getContentResolver();
		mLayoutInflater = LayoutInflater.from(context);
	}

	@SuppressWarnings("deprecation")
	public void requery() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI, FROM, SELECT, null, SORT_ORDER);
		Cursor oldCursor = getCursor();
		changeCursor(cursor);
		mContext.stopManagingCursor(oldCursor);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor cursor = this.getCursor();
		cursor.moveToPosition(position);
		long dateMilliseconds = cursor.getLong(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DATE));
		String date = MyUtil.getChatTime(dateMilliseconds);
		String message = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
		String jid = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.JID));

		String selection = ChatConstants.JID + " = '" + jid + "' AND "
				+ ChatConstants.DIRECTION + " = " + ChatConstants.INCOMING
				+ " AND " + ChatConstants.DELIVERY_STATUS + " = "
				+ ChatConstants.DS_NEW;// 新消息数量字段
		Cursor msgcursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				new String[] { "count(" + ChatConstants.PACKET_ID + ")",
						ChatConstants.DATE, ChatConstants.MESSAGE }, selection,
				null, SORT_ORDER);
		msgcursor.moveToFirst();
		int count = msgcursor.getInt(0);
		ViewHolder viewHolder;
		if (convertView == null || convertView.getTag(R.drawable.ic_launcher + (int) dateMilliseconds) == null) {
			convertView = mLayoutInflater.inflate(R.layout.recent_listview_item, parent, false);
			viewHolder = buildHolder(convertView, jid);
			convertView.setTag(R.drawable.ic_launcher + (int) dateMilliseconds, viewHolder);
			convertView.setTag(R.string.app_name, R.drawable.ic_launcher + (int) dateMilliseconds);
		} else {
			viewHolder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher + (int) dateMilliseconds);
		}
		viewHolder.jidView.setText(jid);
		viewHolder.msgView.setText(MyUtil.convertNormalStringToSpannableString(mContext, message, true));
		viewHolder.dataView.setText(date);

		if (msgcursor.getInt(0) > 0) {
			viewHolder.msgView.setText(msgcursor.getString(msgcursor
					.getColumnIndex(ChatConstants.MESSAGE)));
			viewHolder.dataView.setText(MyUtil.getChatTime(msgcursor
					.getLong(msgcursor.getColumnIndex(ChatConstants.DATE))));
			viewHolder.unReadView.setText(msgcursor.getString(0));
		}
		viewHolder.unReadView.setVisibility(count > 0 ? View.VISIBLE
				: View.GONE);
		viewHolder.unReadView.bringToFront();
		msgcursor.close();

		return convertView;
	}

	private ViewHolder buildHolder(View convertView, final String jid) {
		ViewHolder holder = new ViewHolder();
		holder.jidView = (TextView) convertView
				.findViewById(R.id.recent_list_item_name);
		holder.dataView = (TextView) convertView
				.findViewById(R.id.recent_list_item_time);
		holder.msgView = (TextView) convertView
				.findViewById(R.id.recent_list_item_msg);
		holder.unReadView = (TextView) convertView.findViewById(R.id.unreadmsg);
		return holder;
	}

	private static class ViewHolder {
		TextView jidView;
		TextView dataView;
		TextView msgView;
		TextView unReadView;
//		Button deleteBtn;
	}
}
