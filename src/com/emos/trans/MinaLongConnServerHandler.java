package com.emos.trans;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinaLongConnServerHandler extends IoHandlerAdapter {

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());

	@Override
	public void sessionOpened(IoSession session) {

		InetSocketAddress remoteAddress = (InetSocketAddress) session
				.getRemoteAddress();
		String clientIp = remoteAddress.getAddress().getHostAddress();
		System.out.println("LongConnect Server opened Session ID ="
				+ String.valueOf(session.getId()));
		System.out.println("Received from IP: " + clientIp);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		System.out.println("-------------------------------------");
		IoBuffer mBuffer = (IoBuffer) message;
		byte[] bufBytes = mBuffer.array();
		int limit = mBuffer.limit();
//		int limit = bufBytes.length;
		System.out.println("");
		
		if(limit < 2){
			System.out.println("limit < 2");
			return;
		}
		short type, len;
		
		type = (short) bufBytes[0];
		System.out.println("type = " + type + "  limit: " + mBuffer.limit());
		switch (type) {
		case 0x01:
			System.out.println("heart Beat.");
			/* it's a heart Beat Msg */
			return;
			
		case 0x03:
			System.out.println("login Msg.");
			/* it's a login Msg */
			return;
			
		default:
			break;
		}

		/* get Length of payload */
		short l1 = (short) bufBytes[2];
		short l0 = (short) bufBytes[3];
		l1 <<= 8;
		len = (short) (l1 | l0);
		System.out.println("len : " + len);
		
		if(len > limit - 4){
			System.out.println("only parts of payload received. \nlimit=" + limit + "\tRemaining : " + (len - limit));
		}
		
		handlePayload(bufBytes, 4, limit - 4);
		
//		System.out.println("Message is:" + expression);

	}

	private void handlePayload(byte[] bufBytes, int off, int len) {
		// TODO Auto-generated method stub
		String s = new String(bufBytes, off, len, Charset.forName("UTF-8"));
		System.out.println("s: " + s);
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {

		System.out.println("Disconnectingthe idle.");
		session.close(true);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {

		// close the connection onexceptional situation

		System.out.println("in exception.");
		System.out.println(cause.getMessage());
		logger.warn(cause.getMessage(), cause);
		session.close(true);
	}

}
