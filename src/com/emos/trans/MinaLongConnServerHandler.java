package com.emos.trans;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashSet;
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
import com.emos.trans.logic.TransClientLogic;

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
		MHolder holder = new MHolder();
		holder.session = session;
		GlobalMap.getSsidHolderMap().put(session.getId(), holder);

		/* 加入计时器，连接XX秒后未登录的直接断开连接 */
		MLoginTimer.schedule(new MLoginTimerTask(holder),
				MCommon.DELAY_LOGIN_CHECK_TASK);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		/* check for remaining */
		long id = session.getId();
		MHolder holder = GlobalMap.getSsidHolderMap().get(id);
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

	private void processMsgBytes(byte[] bufBytes, int limit, MHolder holder) {
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
					MLog.logger.trace("heart Beat.");
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

	private void handlePayload(MHolder holder, int type, byte[] bufBytes,
			int off, int len) {
		handlePayload2(holder, type, null, 0, 0, bufBytes, off, len);
	}

	private void handlePayload2(MHolder holder, int type, byte[] preBytes,
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

		String strIn = new String(buffer, 0, preLen + len);
		try {
			/* building Json Object */
			JSONObject jsonMsg = new JSONObject(strIn);

			switch (type) {
			/**********************************************
			 * Logic For Phone;
			 **********************************************/
			/* 登录验证，不通过则更新schedule=60秒 */
			case MCommon.MSG_PHONE_LOGIN:
				MLog.logger.debug(" -- MSG_PHONE_LOGIN -- ");
				String user = jsonMsg.getString("USER");
				String pswd = jsonMsg.getString("PSWD");
				/* 检查该用户密码是否正确。 */
				TransClientLogic.checkUserPswd(user, pswd);
				/* 检查该用户是否登陆过未清理，进行资源清理。或者通知下线。 */
				if (TransClientLogic.userIsAlive(user)) {
					/* 清理旧的USER资源，可以发送登录冲突 */
					// 根据user查到sessionID
					Long ssid = GlobalMap.getUserSsidMap().get(user);
					// 根据sessionID查到holder，发出冲突push消息，然后断开session，删除sessionID为KEY的值，删除user-ssid
					if (ssid != null) {
						MHolder holder_01 = GlobalMap.getSsidHolderMap().get(
								ssid);
						if (holder_01 != null) {
							MLog.logger.debug(user
									+ " already exist. Cleaning....");
							holder_01.session.close(false);
							GlobalMap.getSsidHolderMap().remove(ssid);
							GlobalMap.getUserSsidMap().remove(user);
						}
					}
				}
				// 将user-ssid放入MAP。
				GlobalMap.getUserSsidMap().put(user, holder.session.getId());
				holder.isLogin = true;
				holder.type = MCommon.MHOLDER_TYPE_PHONE;
				holder.id = user;
				// TODO 发送确认消息给客户端。
				break;

			/* 登出，断开连接，清除User的资源及绑定关系 */
			case MCommon.MSG_PHONE_LOGOUT:
				MLog.logger.debug(" -- MSG_PHONE_LOGOUT -- ");
				/* 断开连接，清理旧的USER资源 */
				break;

			/* 注册启动的安防主控的UUID */
			case MCommon.MSG_PHONE_REG_UUID:
				MLog.logger.debug(" -- MSG_PHONE_REG_UUID -- ");
				/* 查询是否注册？是的话删除之前的，否则加直接加入uuid-user */
				String uuid_01 = jsonMsg.getString("UUID");
				String user_01 = jsonMsg.getString("USER");
				Set<String> set = GlobalMap.getUidUserMap().get(uuid_01);
				if (set != null) {
					set.add(user_01);
				} else {
					// UUID not bound
					JSONObject res01 = new JSONObject();
					res01.put("RES", MCommon.RES_HOME_NOT_REG);
					Utils.sendMsgStr(session, MCommon.MSG_HOME_ALARM_RE, res01.toString());
				}
				break;

			/* 撤除注册的主控UUID，将不会推送，从MAP<UUID,SET<USER>>里删除 */
			case MCommon.MSG_PHONE_UNREG_UUID:
				MLog.logger.debug(" -- MSG_PHONE_UNREG_UUID -- ");
				String uuid_02 = jsonMsg.getString("UUID");
				String user_02 = jsonMsg.getString("USER");
				Set<String> set_02 = GlobalMap.getUidUserMap().get(uuid_02);
				if (set_02 != null) {
					set_02.remove(user_02);
				} else {
					// UUID not bound
				}
				break;

			/**********************************************
			 * Logic for Home;
			 **********************************************/
			/* 主控发过来的消息，进行UUID登记，存入MAP<UUID,SET<USER>>, 作为key */
			case MCommon.MSG_HOME_REG:
				MLog.logger.debug(" -- MSG_HOME_REG -- ");
				String homeUuid = jsonMsg.getString("UUID");
				// register MAP<UUID,List<User> >
				Set<String> listUser = new HashSet<String>();
				GlobalMap.getUidUserMap().put(homeUuid, listUser);
				holder.type = MCommon.MHOLDER_TYPE_HOME;
				holder.id = homeUuid;
				break;

			/* 主控发过来的消息，进行UUID撤销，清除MAP<UUID,SET<USER>>中的key-value */
			case MCommon.MSG_HOME_UNREG:
				MLog.logger.debug(" -- MSG_HOME_UNREG -- ");
				break;

			/**********************************************
			 * Logic for Push;
			 **********************************************/
			case MCommon.MSG_HOME_ALARM: /*  */
				MLog.logger.debug(" -- MSG_HOME_ALARM -- ");
				String toPushUuid = jsonMsg.getString("UUID");
				// get List<USER>
				Set<String> listToPushUser = GlobalMap.getUidUserMap().get(
						toPushUuid);
				Map<String, Long> sessionIdMap = GlobalMap.getUserSsidMap();
				Map<Long, MHolder> holderMap = GlobalMap.getSsidHolderMap();
				// for each USER
				if (listToPushUser == null) {
					MLog.logger.debug("UUID not register.");
					break;
				}
				for (String toPushUser : listToPushUser) {
					MLog.logger.debug("pushing..." + toPushUser);
					Long sessionID = sessionIdMap.get(toPushUser);
					if (sessionID != null) {
						MHolder holderUser = holderMap.get(sessionID);
						if (holderUser == null) {
							MLog.logger.debug("USER session not found.");
							listToPushUser.remove(sessionID);
							continue;
						}
						// push Message
						byte[] toPushBytes = LogicUtils.packMsg(
								(short) MCommon.MSG_PUSH_ALARM, preBytes,
								preOff, preLen, bufBytes, off, len);
						IoBuffer mIoBuffer = IoBuffer.wrap(toPushBytes);
						holderUser.session.write(mIoBuffer);
					} else {
						// session == null, clean!!!!
					}
				}// end of for
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
		cause.printStackTrace();
		session.close(true);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		/* remove HOLDER from MAP<sessionID,MHolder> */
		Map<Long, MHolder> map = GlobalMap.getSsidHolderMap();
		long id = session.getId();
		MHolder holder = map.get(id);
		if (holder != null) {
			if (holder.type == MCommon.MHOLDER_TYPE_HOME) {
				MLog.logger.debug("home disconnected uuid = " + holder.id);
			} else if (holder.type == MCommon.MHOLDER_TYPE_PHONE) {
				MLog.logger.debug("PHONE disconnected user = " + holder.id);
				GlobalMap.getUserSsidMap().remove(holder.id);
			} else {
				MLog.logger.debug("unkown MHOLDER_TYPE.");
				return;
			}
		}
		map.remove(id);
		MLog.logger.info(holder.id + " closed. ssid = " + id);
	}

}
