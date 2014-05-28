package com.emos.trans;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emos.trans.logic.GlobalMap;
import com.emos.trans.logic.TransClientLogic;

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
		GlobalMap.getSsidHolderMap().put(session.getId(), holder);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		/* check for remaining */
		long id = session.getId();
		MHolder holder = GlobalMap.getSsidHolderMap().get(id);
		if(holder == null){
			System.out.println("messageReceived && holder==null");
			return;
		}

		IoBuffer mBuffer = (IoBuffer) message;
		byte[] bufBytes = mBuffer.array();
		int limit = mBuffer.limit();
		System.out.println("limit : " + limit
				+ "-----------------------------------------");

		processMsgBytes(bufBytes, limit, holder);
	}

	private void processMsgBytes(byte[] bufBytes, int limit, MHolder holder) {
		int ptr = 0;
		short type = 0, pLen;
		int remaining = holder.msgRemaining;
		while (ptr < limit) {
			// System.out.print("#");
			if (remaining == 0) {
				/* no remaining */

				/* read 2 bytes of head */
				type = bufBytes[ptr];
				if (limit - ptr == 1 && type != 0x01) {
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
					sendHeartBeat(holder.session);
					continue;
				}
				if (limit == ptr) {
					System.out.print("set-2 ");
					holder.msgRemaining = -2;
					// continue;
					return;
				}

				/* read 2 bytes of length */
				if (limit - ptr == 1) {
					holder.msgRemaining = -3;
					holder.msgPreBytes = new byte[1];
					holder.msgPreBytes[0] = bufBytes[ptr];
					holder.msgPreType = type;
					ptr++;
					// continue;
					return;
				}
				/* get Length of payload */
				short l1 = (short) bufBytes[ptr];
				short l0 = (short) bufBytes[ptr + 1];
				l1 <<= 8;
				pLen = (short) (l1 | l0);
				// System.out.println("pLen : " + pLen);
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
					handlePayload(holder.session, type, bufBytes, ptr, pLen);
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
						handlePayload2(holder.session, holder.msgPreType,
								holder.msgPreBytes, 0,
								holder.msgPreBytes.length, bufBytes, ptr,
								remaining);
					} else {
						/* no prebytes Payload */
						handlePayload(holder.session, holder.msgPreType,
								bufBytes, ptr, remaining);
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
				ptr++;
				if(type == 0x01){
					//HeartBeat MSG
					sendHeartBeat(holder.session);
				}else{
					remaining = -2;
					holder.msgRemaining = -2;
					 holder.msgPreType = type;
				}

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
				// System.out.println("pLen : " + pLen);
				++ptr;
			}
		}/* end of while */
	}

	private void handlePayload(IoSession session, int type, byte[] bufBytes,
			int off, int len) {
		handlePayload2(session, type, null, 0, 0, bufBytes, off, len);
	}

	private void handlePayload2(IoSession session, int type, byte[] preBytes,
			int preOff, int preLen, byte[] bufBytes, int off, int len) {
		int rtvl;
		
		/* combine buffer. */
		byte[] buffer = new byte[preLen + len];
		for (int i = 0, j = preOff; i < preLen; i++, j++) {
			buffer[i] = preBytes[j];
		}
		for (int i = preLen, j = off; i < preLen + len; i++, j++) {
			buffer[i] = bufBytes[j];
		}

		String strIn = new String(buffer, 0, preLen + len);
		try {
			/* building Json Object */
			JSONObject jsonMsg = new JSONObject(strIn);

			switch (type) {
			/*
			 * login MSG ???
			 */
			case 0x03:
				System.out.println("login Msg.");
				/* it's a login Msg */

				break;

			/*
			 * for Testing
			 */
			case 0x2C:
				String s;
				if (preBytes != null) {
					s = new String(preBytes, preOff, preLen,
							Charset.forName("GBK"));
					// s += " + ";
					s += new String(bufBytes, off, len, Charset.forName("GBK"));
				} else {
					s = new String(bufBytes, off, len, Charset.forName("GBK"));
				}
				System.out.println(s);
				try {
					JSONObject json = new JSONObject(s);
					int count = json.getInt("STR");
					byte[] strByte = Integer.toString(count).getBytes();
					int lenStr = strByte.length;
					byte[] byteArray = new byte[4 + lenStr];
					byteArray[0] = 0x00;
					byteArray[1] = 0x00;
					byteArray[2] = (byte) ((lenStr & 0x0000FF00) >> 8);
					byteArray[3] = (byte) (lenStr & 0x000000FF);
					for (int i = 4, j = 0; i < lenStr + 4; j++, i++) {
						byteArray[i] = strByte[j];
					}
					IoBuffer ioBuf = IoBuffer.wrap(byteArray);
					session.write(ioBuf);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;

			/*
			 * 0x10 Client连接Server时，用户名密码验证，以及确认UUID
			 */
			case 0x10:
				System.out.println("case 0x10");
				String user = jsonMsg.getString("USER");
				String uuid = jsonMsg.getString("UUID");
				String pswd = jsonMsg.getString("PSWD");
				rtvl = TransClientLogic.checkUserPswd(user, pswd);
				if(rtvl < 0){
					System.out.println("pswd Wrong!");
				}
				//register MAP<UUID,List<USER> >
				Set<String> list = GlobalMap.getUidUserMap().get(uuid);
				if(list != null){
					list.add(user);
				}else{
					//TODO send back msg.
					System.out.println("UUID not running.");
				}
				//register MAP<USER,sessionID>
				GlobalMap.getUserSsidMap().put(user, session.getId());
				break;

			/*
			 * 0x12 Client退出推送
			 */
			case 0x12:
				System.out.println("case 0x12");
				break;

			/*
			 * 0x20 Home端首次连接发送，UUID确认。
			 */
			case 0x20:
				System.out.println("case 0x20");
				String homeUuid = jsonMsg.getString("UUID");
				//register MAP<UUID,List<User> >
				Set<String> listUser = new HashSet<String>();
				GlobalMap.getUidUserMap().put(homeUuid, listUser);
				break;

			/*
			 * 0x22 Home端推送消息，格式{json}
			 */
			case 0x22:
				System.out.println("case 0x22");
				String toPushUuid = jsonMsg.getString("UUID");
				//get List<USER>
				Set<String> listToPushUser = GlobalMap.getUidUserMap().get(toPushUuid);
				Map<String, Long> sessionIdMap = GlobalMap.getUserSsidMap();
				Map<Long, MHolder> holderMap = GlobalMap.getSsidHolderMap();
				//for each USER
				if(listToPushUser == null){
					System.out.println("UUID not register.");
					break;
				}
				for (String toPushUser : listToPushUser) {
					Long sessionID = sessionIdMap.get(toPushUser);
					if(sessionID != null){
						System.out.println("pushing..." + toPushUser);
						MHolder holder = holderMap.get(sessionID);
						if(holder==null){
							System.out.println("USER session not found.");
							continue;
						}
						//push Message			
						byte[] strInBytes = strIn.getBytes();
						byte[] toPushBytes = new byte[strInBytes.length + 4];
						toPushBytes[0] = 0x23;
						toPushBytes[1] = 0x23;
						toPushBytes[2] = (byte) ((strInBytes.length & 0x0000FF00) >> 8);
						toPushBytes[3] = (byte) (strInBytes.length & 0x000000FF);
						for(int i=4,j=0;i<strInBytes.length+4;i++,j++){
							toPushBytes[i] = strInBytes[j];
						}
						IoBuffer mIoBuffer = IoBuffer.wrap(toPushBytes);
						holder.session.write(mIoBuffer);
					}else{
						//session == null,  clean!!!!
					}
				}//end of for				
				break;

			default:
				break;
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	}
	
	private void sendHeartBeat(IoSession session){
		byte[] heartBeatMsg = new byte[2];
		heartBeatMsg[0] = 0x02;
		heartBeatMsg[1] = 0x00;
		IoBuffer mIoBuffer = IoBuffer.wrap(heartBeatMsg);
		session.write(mIoBuffer);
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {

		System.out.println("Disconnectingthe idle.");
		session.close(true);

		/* remove HOLDER from MAP<sessionID,MHolder> */
		GlobalMap.getSsidHolderMap().remove(session.getId());
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {

		// close the connection onexceptional situation
		System.out.println("in exception.");
		System.out.println(cause.getMessage());
		
		/* remove This session in the map */
//		MinaLongConnServer.holderMap.put(session.getId(), null);
		
		/* remove HOLDER from MAP<sessionID,MHolder> */
		GlobalMap.getSsidHolderMap().remove(session.getId());
		// logger.warn(cause.getMessage(), cause);
		session.close(true);
	}

}
