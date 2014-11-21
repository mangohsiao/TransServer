package com.emos.trans.msgInterface;

import com.emos.trans.logic.SHolder;

public interface IMsgHandler {
	public void handleMsg(SHolder holder, int type, byte[] buffer, int len);
}
