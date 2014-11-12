package com.xchat.base;

public interface IConnectionStatusCallback {
	public void connectionStatusChanged(int connectedState, String reason);
}
