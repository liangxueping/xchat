package com.xchat.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.xchat.activity.ChatActivity;
import com.xchat.activity.R;
import com.xchat.adapter.RecentChatAdapter;
import com.xchat.db.ChatProvider;
import com.xchat.db.ChatProvider.ChatConstants;
import com.xchat.service.XChatService;
import com.xchat.system.L;
import com.xchat.utils.MyUtil;
import com.xchat.view.AddRosterItemDialog;
import com.xchat.view.CustomDialog;

public class RecentChatFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener {

	private Handler mainHandler = new Handler();
	private ContentObserver mChatObserver = new ChatObserver();
	private ContentResolver mContentResolver;
	private ListView mSwipeListView;
	private RecentChatAdapter mRecentChatAdapter;
	private TextView mTitleView;
	private ImageView mTitleAddView;
	private IFragmentCallBack mFragmentCallBack;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mFragmentCallBack = (IFragmentCallBack) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnHeadlineSelectedListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContentResolver = getActivity().getContentResolver();
		mRecentChatAdapter = new RecentChatAdapter(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();
		mRecentChatAdapter.requery();
		mContentResolver.registerContentObserver(ChatProvider.CONTENT_URI, true, mChatObserver);
	}

	@Override
	public void onPause() {
		super.onPause();
		mContentResolver.unregisterContentObserver(mChatObserver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.recent_chat_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initView(view);
	}

	private void initView(View view) {
		mTitleView = (TextView) view.findViewById(R.id.ivTitleName);
		mTitleView.setText(R.string.recent_chat_fragment_title);
		mTitleAddView = (ImageView) view.findViewById(R.id.ivTitleBtnRightImage);
		mTitleAddView.setImageResource(R.drawable.setting_add_account_white);
		mTitleAddView.setVisibility(View.VISIBLE);
		mTitleAddView.setOnClickListener(this);
		mSwipeListView = (ListView) view.findViewById(R.id.recent_listview);
		mSwipeListView.setEmptyView(view.findViewById(R.id.recent_empty));
		mSwipeListView.setAdapter(mRecentChatAdapter);
//		mSwipeListView.setSwipeListViewListener(mSwipeListViewListener);
		mSwipeListView.setOnItemClickListener(this);
		mSwipeListView.setOnItemLongClickListener(this);

	}

	public void updateRoster() {
		mRecentChatAdapter.requery();
	}

	private class ChatObserver extends ContentObserver {
		public ChatObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			updateRoster();
			L.i("liweiping", "selfChange" + selfChange);
		}
	}

//	BaseSwipeListViewListener mSwipeListViewListener = new BaseSwipeListViewListener() {
//		@Override
//		public void onClickFrontView(int position) {
//			Cursor clickCursor = mRecentChatAdapter.getCursor();
//			clickCursor.moveToPosition(position);
//			String jid = clickCursor.getString(clickCursor.getColumnIndex(ChatConstants.JID));
//			Uri userNameUri = Uri.parse(jid);
//			Intent toChatIntent = new Intent(getActivity(), ChatActivity.class);
//			toChatIntent.setData(userNameUri);
//			toChatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, jid);
//			startActivity(toChatIntent);
//		}
//
//		@Override
//		public void onClickBackView(int position) {
//			mSwipeListView.closeOpenedItems();// 关闭打开的项
//		}
//	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ivTitleBtnRightImage:
			XChatService xxService = mFragmentCallBack.getService();
			if (xxService == null || !xxService.isAuthenticated()) {
				return;
			}
			new AddRosterItemDialog(mFragmentCallBack.getMainActivity(), xxService).show();// 添加联系人
			break;

		default:
			break;
		}
	}

	void removeChatHistory(final String JID) {
		mContentResolver.delete(ChatProvider.CONTENT_URI,
				ChatProvider.ChatConstants.JID + " = ?", new String[] { JID });
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Cursor clickCursor = mRecentChatAdapter.getCursor();
		clickCursor.moveToPosition(position);
		final String jid = clickCursor.getString(clickCursor.getColumnIndex(ChatConstants.JID));
		String userName = MyUtil.getUserNameByID(mFragmentCallBack.getMainActivity().getContentResolver(), jid);
		new CustomDialog.Builder(getActivity())
				.setTitle(R.string.deleteChatHistory_title)
				.setMessage(
						getActivity().getString(R.string.deleteChatHistory_text,
								userName, jid))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								removeChatHistory(jid);
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Cursor clickCursor = mRecentChatAdapter.getCursor();
		clickCursor.moveToPosition(position);
		String jid = clickCursor.getString(clickCursor.getColumnIndex(ChatConstants.JID));
		Uri userNameUri = Uri.parse(jid);
		Intent toChatIntent = new Intent(getActivity(), ChatActivity.class);
		toChatIntent.setData(userNameUri);
		String userName = MyUtil.getUserNameByID(mFragmentCallBack.getMainActivity().getContentResolver(), jid);
		toChatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
		startActivity(toChatIntent);
	}
}
