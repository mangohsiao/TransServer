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

		/* open a session && put it into map && TOBE verify */
		MHolder holder = new MHolder();
		holder.session = session;
		MinaLongConnServer.holderMap.put(session.getId(), holder);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		/* check for remaining */
		long id = session.getId();
		MHolder holder = MinaLongConnServer.holderMap.get(id);

		IoBuffer mBuffer = (IoBuffer) message;
		byte[] bufBytes = mBuffer.array();
		int limit = mBuffer.limit();
//		System.out.println("limit : " + limit + "-----------------------------------------");

		processMsgBytes(bufBytes, limit, holder);
	}

	private void processMsgBytes(byte[] bufBytes, int limit, MHolder holder) {
		int ptr = 0;
		short type = 0, pLen;
		int remaining = holder.msgRemaining;
		while (ptr < limit) {
			System.out.print("#");
			if (remaining == 0) {
				/* no remaining */
				
				/* read 2 bytes of head */
				type = bufBytes[ptr];
				if (limit - ptr == 1 && type!=0x01) {
					// header not whole
					holder.msgRemaining = -1;
					holder.msgPreBytes = new byte[1];
					holder.msgPreBytes[0] = bufBytes[ptr];
					holder.msgPreType = type;
					ptr += 1;
					continue;
				}
				ptr += 2;
				if (type == 0x01) {
					System.out.println("heart Beat.");
					/* it's a heart Beat Msg */
					continue;
				}
				if(limit == ptr){
					System.out.print("set-2 ");
					holder.msgRemaining = -2;
//					continue;
					return;
				}

				/* read 2 bytes of length */
				if (limit - ptr == 1) {
					holder.msgRemaining = -3;
					holder.msgPreBytes = new byte[1];
					holder.msgPreBytes[0] = bufBytes[ptr];
					holder.msgPreType = type;
					ptr ++;
//					continue;
					return;
				}				
				/* get Length of payload */
				short l1 = (short) bufBytes[ptr];
				short l0 = (short) bufBytes[ptr + 1];
				l1 <<= 8;
				pLen = (short) (l1 | l0);
//				System.out.println("pLen : " + pLen);
				ptr += 2;

				// process Payload
				if (pLen > (limit - ptr)) {
					remaining = pLen - (limit - ptr);
					// System.out.println("remaining : " + remaining);
					holder.msgRemaining = remaining;
					holder.msgPreType = type;
					int pre = limit - ptr;
					if (pre != 0) {
						byte[] des = new byte[pre];
						for (int i = 0; i < pre; i++) {
							des[i] = bufBytes[ptr + i];
						}
						holder.msgPreBytes = des;
					} else {
						holder.msgPreBytes = null;
					}
					ptr = limit;
				} else {
					handlePayload(type, bufBytes, ptr, pLen);
					ptr += pLen;
				}
				
			} else if (remaining > 0) {
				System.out.println("r>0 ");
				if (remaining > limit - ptr) {
					// over limit
					System.out.print("remaining > limit\t");
					return;
				} else {
					if (holder.msgPreBytes != null) {
						// handle pre msg && remaining msg
						handlePayload2(holder.msgPreType, holder.msgPreBytes,
								0, holder.msgPreBytes.length, bufBytes, ptr,
								remaining);
					} else {
						/* no prebytes Payload */
						handlePayload(holder.msgPreType, bufBytes, ptr,
								remaining);
					}
					ptr += remaining;
					remaining = 0;
					holder.msgRemaining = 0;
					continue;
				}
			} else if (remaining == -1) {
				/* only read 1 byte. */
				System.out.print("r=-1 ");
				short reserved = bufBytes[ptr];
				ptr ++;
				remaining = -2;
				holder.msgRemaining = -2;
//				holder.msgPreType = (short)holder.msgPreBytes[0];
				
			} else if (remaining == -2) {
				/* only read 1 byte. */
				System.out.print("r=-2 ");
				/* read 2 bytes of length */
				if (limit - ptr == 1) {
					holder.msgRemaining = -3;
					byte[] des = new byte[1];
					des[0] = bufBytes[ptr];
					holder.msgPreBytes = des;
					++ptr;
					continue;
				}
				/* get Length of payload */
				short l1 = (short) bufBytes[ptr];
				short l0 = (short) bufBytes[ptr + 1];
				l1 <<= 8;
				pLen = (short) (l1 | l0);
				System.out.println("r2-len=" + pLen + " ");
				remaining = pLen;
				holder.msgRemaining = pLen;
				holder.msgPreBytes = null;
				ptr += 2;
			} else if (remaining == -3) {
				/* only read 3 byte. */
				/* calculate the Length */
				/* get Length of payload */
				short l1 = (short) holder.msgPreBytes[0];
				short l0 = (short) bufBytes[0];
				l1 <<= 8;
				pLen = (short) (l1 | l0);
				remaining = pLen;
				holder.msgRemaining = pLen;
				System.out.println("r=-3.pl=" + pLen + " ");
				holder.msgPreBytes = null;
//				System.out.println("pLen : " + pLen);
				++ptr;
			}
		}/* end of while */
	}

	private void handlePayload(int type, byte[] bufBytes, int off, int len) {
		handlePayload2(type, null, 0, 0, bufBytes, off, len);
	}

	private void handlePayload2(int type, byte[] preBytes, int preOff,
			int preLen, byte[] bufBytes, int off, int len) {

		switch (type) {
		case 0x03:
			System.out.println("login Msg.");
			/* it's a login Msg */

			break;

		case 0x2C:
			String s;
			if (preBytes != null) {
				s = new String(preBytes, preOff, preLen, Charset.forName("GBK"));
				s += " + ";
				s += new String(bufBytes, off, len, Charset.forName("GBK"));
			} else {
				s = new String(bufBytes, off, len, Charset.forName("GBK"));
			}
			System.out.println(s);

			break;
		default:
			break;
		}
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
		/* remove This session in the map */
		MinaLongConnServer.holderMap.put(session.getId(), null);
		// logger.warn(cause.getMessage(), cause);
		session.close(true);
	}

}
