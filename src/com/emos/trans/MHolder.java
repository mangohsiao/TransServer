package com.emos.trans;

import org.apache.mina.core.session.IoSession;

public class MHolder {
	public static final short TYPE_PHONE = 2;
	public static final short TYPE_HOME = 1;
	
	IoSession session = null;
	short type = 0;		/* 1 is Home , 2 is Phone */
	String id = null;
	
	/* members for Message Processing */
	short msgPreType = 0;
	int msgRemaining = 0;
	byte[] msgPreBytes;
	
	/* For timer, Cleaning Resource without login */
	boolean isLogin = false;
}