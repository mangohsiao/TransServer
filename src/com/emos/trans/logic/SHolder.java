package com.emos.trans.logic;

import org.apache.mina.core.session.IoSession;

public class SHolder{
	public int type = 0;
	public IoSession session = null;
	
	/* members for Message Processing */
	public short msgPreType = 0;
	public int msgRemaining = 0;
	public byte[] msgPreBytes;
	
	/* For timer, Cleaning Resource without login */
	public boolean isLogin = false;

	public IoSession getSession() {
		return session;
	}

	public void setSession(IoSession session) {
		this.session = session;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
}