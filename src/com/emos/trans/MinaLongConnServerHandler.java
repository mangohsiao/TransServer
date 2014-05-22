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
		
		MHolder holder = new MHolder();
		MinaLongConnServer.holderMap.put(session.getId(), holder);		
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		/* check for remaining */
		long id = session.getId();
		MHolder holder = MinaLongConnServer.holderMap.get(id);
		
		System.out.println("----------------" + holder.msgRemaining + "---------------------");
		IoBuffer mBuffer = (IoBuffer) message;
		byte[] bufBytes = mBuffer.array();
		int limit = mBuffer.limit();
//		int limit = bufBytes.length;
		System.out.println("limit: " + mBuffer.limit());
		
//		if(limit < 2){
//			System.out.println("limit < 2");
//			return;
//		}
		
		processMsgBytes(bufBytes, limit, holder);
		
/*		short type, len;		
		type = (short) bufBytes[0];
		switch (type) {
		case 0x01:
			System.out.println("heart Beat.");
			 it's a heart Beat Msg 
			return;
			
		case 0x03:
			System.out.println("login Msg.");
			 it's a login Msg 
			return;
			
		default:
			break;
		}

		 get Length of payload 
		short l1 = (short) bufBytes[2];
		short l0 = (short) bufBytes[3];
		l1 <<= 8;
		len = (short) (l1 | l0);
		System.out.println("len : " + len);
		
		if(len > limit - 4){
			System.out.println("only parts of payload received. \nlimit=" + limit + "\tRemaining : " + (len - limit));
		}
		
		handlePayload(bufBytes, 4, limit - 4);*/
		
//		System.out.println("Message is:" + expression);

	}

	private void processMsgBytes(byte[] bufBytes, int limit, MHolder holder){
		int ptr = 0;
		short type, pLen;
		int remaining = holder.msgRemaining;
		while(ptr < limit){
			if(remaining > 0){
				if(remaining > limit){
					//over limit
					return;
				}else{
					handlePayload(bufBytes, ptr, remaining);
					ptr += remaining;
					remaining = 0;
					holder.msgRemaining = 0;
					continue;
				}
			}
			type = bufBytes[ptr];
			ptr += 2;			
			switch (type) {
			case 0x01:
				System.out.println("heart Beat.");
				/* it's a heart Beat Msg */
				break;
				
			case 0x03:
				System.out.println("login Msg.");
				/* it's a login Msg */
				break;

			default:
				/* get Length of payload */
				short l1 = (short) bufBytes[ptr];
				short l0 = (short) bufBytes[ptr+1];
				l1 <<= 8;
				pLen = (short) (l1 | l0);
				System.out.println("pLen : " + pLen);
				ptr += 2;
				
				if(pLen > (limit - ptr)){
					remaining = pLen - (limit - ptr);
					System.out.println("remaining : " + remaining);
					handlePayload(bufBytes, ptr, limit-ptr);
					holder.msgRemaining = remaining;
					ptr = limit;
				}else{
					handlePayload(bufBytes, ptr, pLen);
					ptr += pLen;
				}				
				break;
			}
		}
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
