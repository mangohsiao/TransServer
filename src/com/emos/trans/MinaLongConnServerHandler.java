package com.emos.trans;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.rmi.CORBA.Util;
import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;

import com.emos.trans.log.MLog;
import com.emos.trans.logic.GlobalMap;
import com.emos.trans.logic.LogicUtils;
import com.emos.trans.logic.SHolder;
import com.emos.trans.logic.SessionMap;
import com.emos.trans.logic.TransClientLogic;
import com.emos.trans.msgInterface.MsgDispatcher;
import com.emos.trans.pojo.HomeTemp;
import com.emos.trans.pojo.UserTemp;
import com.emos.trans.util.DBHelper;

public class MinaLongConnServerHandler extends IoHandlerAdapter {

	// private final Logger logger = (Logger)
	// LoggerFactory.getLogger(getClass());

	@Override
	public void sessionOpened(IoSession session) {
		InetSocketAddress remoteAddress = (InetSocketAddress) session
				.getRemoteAddress();
		String clientIp = remoteAddress.getAddress().getHostAddress();
		MLog.logger.info("LongConnect Server opened Session ID ="
				+ String.valueOf(session.getId()));
		MLog.logger.info("Received from IP: " + clientIp);
		/* open a session && put it into map && TOBE verify */
		// MHolder holder = new MHolder();
		// holder.session = session;
		// GlobalMap.getSsidHolderMap().put(session.getId(), holder);
		/* for DB */
		SHolder sholder = new SHolder();
		sholder.setSession(session);
		SessionMap.getMap().put(session.getId(), sholder);

		/* 加入计时器，连接XX秒后未登录的直接断开连接 */
		MLoginTimer.schedule(new MLoginDBTimerTask(sholder),
				MCommon.DELAY_LOGIN_CHECK_TASK);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		/* check for remaining */
		Long id = session.getId();
		SHolder holder = SessionMap.getMap().get(id);
		if (holder == null) {
			MLog.logger.debug("messageReceived && holder==null");
			return;
		}

		IoBuffer mBuffer = (IoBuffer) message;
		byte[] bufBytes = mBuffer.array();
		int limit = mBuffer.limit();
		MLog.logger.trace("limit : " + limit
				+ "-----------------------------------------");

		processMsgBytes(bufBytes, limit, holder);
	}

	private void processMsgBytes(byte[] bufBytes, int limit, SHolder holder) {
		int ptr = 0;
		short type = 0;
		int pLen;
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
					MLog.logger.debug("HB " + holder.session.getId());
					/* it's a heart Beat Msg */
					sendHeartBeat(holder.session);
					continue;
				}
				if (limit == ptr) {
					MLog.logger.trace("set-2 ");
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
				pLen = (bufBytes[ptr] & 0xff) << 8 | bufBytes[ptr + 1] & 0xff;
				MLog.logger.trace("pLen : " + pLen);
				ptr += 2;

				// process Payload
				if (pLen > (limit - ptr)) {
					remaining = pLen - (limit - ptr);
					// MLog.logger.trace("remaining : " + remaining);
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
					handlePayload(holder, type, bufBytes, ptr, pLen);
					ptr += pLen;
				}

			} else if (remaining > 0) {
				MLog.logger.trace("r>0 ");
				if (remaining > limit - ptr) {
					// over limit
					System.out.print("remaining > limit\t");
					return;
				} else {
					if (holder.msgPreBytes != null) {
						// handle pre msg && remaining msg
						handlePayload2(holder, holder.msgPreType,
								holder.msgPreBytes, 0,
								holder.msgPreBytes.length, bufBytes, ptr,
								remaining);
					} else {
						/* no prebytes Payload */
						handlePayload(holder, holder.msgPreType, bufBytes, ptr,
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
				// TODO reserved 还未解析。
				// short reserved = bufBytes[ptr];
				ptr++;
				if (type == 0x01) {
					// HeartBeat MSG
					sendHeartBeat(holder.session);
				} else {
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
				pLen = (bufBytes[ptr] & 0xff) << 8 | bufBytes[ptr + 1] & 0xff;
				MLog.logger.trace("r2-len=" + pLen + " ");
				remaining = pLen;
				holder.msgRemaining = pLen;
				holder.msgPreBytes = null;
				ptr += 2;
			} else if (remaining == -3) {
				/* only read 3 byte. */
				/* calculate the Length */
				/* get Length of payload */
				pLen = (holder.msgPreBytes[0] & 0xff) << 8 | bufBytes[0] & 0xff;
				remaining = pLen;
				holder.msgRemaining = pLen;
				MLog.logger.trace("r=-3.pl=" + pLen + " ");
				holder.msgPreBytes = null;
				// MLog.logger.trace("pLen : " + pLen);
				++ptr;
			}
		}/* end of while */
	}

	private void handlePayload(SHolder holder, int type, byte[] bufBytes,
			int off, int len) {
		handlePayload2(holder, type, null, 0, 0, bufBytes, off, len);
	}

	private void handlePayload2(SHolder holder, int type, byte[] preBytes,
			int preOff, int preLen, byte[] bufBytes, int off, int len) {
		int rtvl;
		IoSession session = holder.session;
		/* combine buffer. */
		MLog.logger.debug("preLen = " + preLen + " len = " + len);
		byte[] buffer = new byte[preLen + len];
		for (int i = 0, j = preOff; i < preLen; i++, j++) {
			buffer[i] = preBytes[j];
		}
		for (int i = preLen, j = off; i < preLen + len; i++, j++) {
			buffer[i] = bufBytes[j];
		}

		/*
		 * buffer为消息缓存，其长度为preLen+len，类型为type，holder包含所需环境。
		 */
		MsgDispatcher.processMsg(holder, type, buffer, preLen + len);

		if (true)
			return;

		String strIn = new String(buffer, 0, preLen + len);
		try {
			/* building Json Object */
			JSONObject jsonMsg = new JSONObject(strIn);

			switch (type) {
			/**********************************************
			 * Logic For Phone;
			 **********************************************/

			

			/**********************************************
			 * Logic for Push;
			 **********************************************/
			case MCommon.MSG_HOME_ALARM: /*  */
				break;
			case MCommon.MSG_HOME_UPDATE: /*  */
				MLog.logger.debug(" -- MSG_HOME_UPDATE -- ");
				break;
			case MCommon.MSG_HOME_SYNC: /*  */
				MLog.logger.debug(" -- MSG_HOME_SYNC -- ");
				break;

			default:
				/* Invalid format message */
				MLog.logger
						.error("Invalid format message. MSG_TYPE = {}", type);
				break;
			}
			MLog.logger.info(strIn);
		} catch (JSONException e1) {
			// JOSN格式解析出错。
			e1.printStackTrace();
			MLog.logger
					.info("Json Parsing Error. Message received error. Wrong Message.");
		}
	}

	private void sendHeartBeat(IoSession session) {
		byte[] heartBeatMsg = new byte[2];
		heartBeatMsg[0] = 0x02;
		heartBeatMsg[1] = 0x00;
		IoBuffer mIoBuffer = IoBuffer.wrap(heartBeatMsg);
		session.write(mIoBuffer);
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {

		if (status == IdleStatus.READER_IDLE) {
			MLog.logger.info("Reader idle.");
		} else if (status == IdleStatus.WRITER_IDLE) {
			MLog.logger.info("Writer idle.");
		} else if (status == IdleStatus.BOTH_IDLE) {

		}
		session.close(true);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		// close the connection on exceptional situation
		MLog.logger.debug("in exception. - " + cause.getMessage());
		// cause.printStackTrace();
		session.close(true);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		/* for DB */
		Long sid = session.getId();
		SHolder sholder = SessionMap.getMap().get(sid);
		switch (sholder.getType()) {
		case 0:
			break;
		case 1:
			List<UserTemp> list = DBHelper.listUserBySsid(sid);
			if (list.size() > 0) {
				UserTemp userObj = list.get(0);
				DBHelper.deleteUser(userObj);
				MLog.logger.debug("delete user " + userObj.getUserName());
			}
			break;
		case 2:
			List<HomeTemp> list_h02 = DBHelper.listHomeBySsid(sid);
			if (list_h02.size() > 0) {
				HomeTemp homeObj = list_h02.get(0);
				if (session.getId() != homeObj.getSessionId())
					break;
				DBHelper.deleteHome(homeObj);
				MLog.logger.debug("delete home " + homeObj.getHomeUuid());
			}
			break;
		default:
			break;
		}
	}

}
