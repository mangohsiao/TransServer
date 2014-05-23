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

		System.out.println("limit : " + limit);

		processMsgBytes(bufBytes, limit, holder);
	}

	private void processMsgBytes(byte[] bufBytes, int limit, MHolder holder) {
		int ptr = 0;
		short type, pLen;
		int remaining = holder.msgRemaining;
		while (ptr < limit) {
			switch (remaining) {
			case 0:
				/* remaining == 0 , read HEAD */
				System.out.println("remaining == 0");
				type = (short) bufBytes[ptr];
				if (limit - ptr == 1) {
					holder.msgRemaining = -1;
					holder.msgPreType = type;
					byte[] des = new byte[1];
					des[0] = bufBytes[ptr];
					holder.msgPreBytes = des;
					ptr++;
					continue;
				}
				ptr += 2;
				if(type == 0x01){
					System.out.println("heart beat");
					continue;
				}
				
				if (limit == ptr) {
					holder.msgRemaining = -2;
					holder.msgPreType = type;
					holder.msgPreBytes = null;
					continue;
				}
				if (limit - ptr == 1) {
					holder.msgRemaining = -3;
					holder.msgPreType = type;
					byte[] des = new byte[1];
					des[0] = bufBytes[ptr];
					holder.msgPreBytes = des;
					ptr++;
					continue;
				}
				short l1 = (short) bufBytes[ptr];
				short l0 = (short) bufBytes[ptr + 1];
				l1 <<= 8;
				pLen = (short) (l1 | l0);
				ptr += 2;
				if (limit - ptr < pLen) {
					holder.msgRemaining = pLen - (limit - ptr);
					holder.msgPreType = type;
					byte[] des = new byte[limit - ptr];
					for (int i = 0; ptr < limit; ptr++, i++) {
						des[i] = bufBytes[ptr];
					}
					holder.msgPreBytes = des;
					continue;
				} else {
					handlePayload(type, bufBytes, ptr, pLen);
					ptr += pLen;
				}
				break;

			case -1:
				/* remaining == -1 , to read part of HEAD */
				System.out.println("remaining == -1");
				// read reserved.
				ptr++;
				remaining = -2;
				holder.msgRemaining = -2;
				break;

			case -2:
				/* remaining == -2 , to read length */
				System.out.println("remaining == -2");
				if (limit - ptr == 1) {
					holder.msgRemaining = -3;
					byte[] des = new byte[1];
					des[0] = bufBytes[ptr];
					holder.msgPreBytes = des;
					ptr++;
					continue;
				}
				short l01 = (short) bufBytes[ptr];
				short l00 = (short) bufBytes[ptr + 1];
				l01 <<= 8;
				pLen = (short) (l01 | l00);
				ptr += 2;

				if (limit - ptr < pLen) {
					holder.msgRemaining = pLen - (limit - ptr);
					byte[] des = new byte[limit - ptr];
					for (int i = 0; ptr < limit; ptr++, i++) {
						des[i] = bufBytes[ptr];
					}
					holder.msgPreBytes = des;
					continue;
				} else {
					handlePayload(holder.msgPreType, bufBytes, ptr, pLen);
					ptr += pLen;
				}
				break;

			case -3:
				/* remaining == -3 , to read part of length */
				System.out.println("remaining == -3");
				short l0_1 = (short) holder.msgPreBytes[0];
				short l0_0 = (short) bufBytes[ptr];
				l0_1 <<= 8;
				pLen = (short) (l0_1 | l0_0);
				ptr++;

				if (limit - ptr < pLen) {
					holder.msgRemaining = pLen - (limit - ptr);
					byte[] des = new byte[limit - ptr];
					for (int i = 0; ptr < limit; ptr++, i++) {
						des[i] = bufBytes[ptr];
					}
					holder.msgPreBytes = des;
					continue;
				} else {
					handlePayload(holder.msgPreType, bufBytes, ptr, pLen);
					ptr += pLen;
				}
				break;

			default:
				/* remaining > 0 */
				System.out.println("remaining > 0");
				if (remaining > limit - ptr) {
					System.out.print("r>l-p ");
				} else {
					handlePayload2(holder.msgPreType, holder.msgPreBytes, 0,
							holder.msgPreBytes.length, bufBytes, ptr, remaining);
					remaining = 0;
					holder.msgRemaining = 0;
					holder.msgPreBytes = null;
				}
				break;
			}

		}
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
