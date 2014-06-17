package com.emos.trans;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.json.JSONObject;

public class Utils {
	public static void sendMsgJson(IoSession session, int type, JSONObject jObj) {
		sendMsgStr(session, type, jObj.toString());
	}

	public static void sendMsgStr(IoSession session, int type, String str) {
		byte[] strBytes = str.getBytes();
		int len = strBytes.length;
		byte[] buf = new byte[4 + len];
		buf[0] = (byte) type;
		buf[1] = 0x00;
		buf[2] = (byte) ((len >> 8) & 0xff);
		buf[3] = (byte) (len & 0xff);
		for(int i =4,j=0;i<len+4;i++,j++){
			buf[i] = strBytes[j];
		}
		IoBuffer ioBuf = IoBuffer.wrap(buf);
		session.write(ioBuf);
	}
}
