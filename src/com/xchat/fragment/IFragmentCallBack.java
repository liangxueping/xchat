package com.xchat.fragment;

import com.xchat.activity.MainActivity;
import com.xchat.service.XChatService;

public interface IFragmentCallBack {
	public XChatService getService();

	public MainActivity getMainActivity();
}
