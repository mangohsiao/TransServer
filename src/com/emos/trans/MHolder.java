package com.emos.trans;

import org.apache.mina.core.session.IoSession;

public class MHolder {
	IoSession session = null;
	
	/* members for Message Processing */
	short msgPreType = 0;
	int msgRemaining = 0;
	byte[] msgPreBytes;
	
	/* for timer, Cleaning Resource without login */
	//Timertask. login timer.
	//
	
	/*  */
}