package com.emos.trans;

import org.apache.mina.core.session.IoSession;

public class MHolder {
	IoSession session = null;
	short msgPreType = 0;
	int msgRemaining = 0;
	byte[] msgPreBytes;
}