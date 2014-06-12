package com.emos.trans;

import org.apache.mina.core.session.IoSession;

public class MHolder {
	IoSession session = null;
	short type = 0;		/* 1 is Home , 2 is Phone */
	
	/* members for Message Processing */
	short msgPreType = 0;
	int msgRemaining = 0;
	byte[] msgPreBytes;
	
	/* For timer, Cleaning Resource without login */
	boolean isLogin = false;
}