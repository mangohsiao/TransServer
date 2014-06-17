package com.emos.trans.logic;

public class LogicUtils {
	private static final int HEAD_LEN = 4;
	
	public static byte[] packMsg(short type, byte[] preBytes, int preOff,
			int preLen, byte[] bufBytes, int off, int len) {
		int payloadLen = preLen + len;
		byte[] des = new byte[HEAD_LEN + payloadLen];
		des[0] = (byte) type;
		des[1] = (byte) 0x00; /* reserved */
		des[2] = (byte) ((payloadLen & 0x0000FF00) >> 8);
		des[3] = (byte) (payloadLen & 0x000000FF);
		for (int i = HEAD_LEN, j = preOff; j < preOff + preLen; i++, j++) {
			des[i] = preBytes[j];
		}
		for (int m = HEAD_LEN + preLen, n = off; n < off + len; m++, n++) {
			des[m] = bufBytes[n];
		}
		return des;
	}
}
