package com.emos.trans;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
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
import com.emos.trans.logic.DataMap;
import com.emos.trans.logic.GlobalMap;
import com.emos.trans.logic.Home;
import com.emos.trans.logic.LogicUtils;
import com.emos.trans.logic.SHolder;
import com.emos.trans.logic.SessionMap;
import com.emos.trans.logic.TransClientLogic;
import com.emos.trans.logic.User;
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
//		MHolder holder = new MHolder();
//		holder.session = session;
//		GlobalMap.getSsidHolderMap().put(session.getId(), holder);
		
		/* DB version */
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
				if(TransClientLogic.checkUserPswd(user, pswd) < 0){
					/* 登录失败  */
					break;
				}
				Long ssid_old,ssid_new;
				
				/* 检查该用户是否登陆过未清理，进行资源清理。或者通知下线。 */
				/*
				 * DB version.
				 */
				/*
				List<UserTemp> list = DBHelper.listUserByName(user);
				ssid_new = session.getId();
				UserTemp userNew = new UserTemp(user);
				userNew.setSessionId(ssid_new);
				if (list.size() > 0) {
					UserTemp usertemp = list.get(0);
					ssid_old = usertemp.getSessionId();
					MLog.logger.debug("DB - user exist. " + ssid_old);
					DBHelper.updateUser(userNew);	//更新数据库
					// 处理旧的连接  
					SessionMap.getMap().get(ssid_old).getSession().close(false);					
				} else {
					// 插入user到DB表
					DBHelper.insertUser(userNew);
				}
				holder.setType(1);
				holder.isLogin = true;
				*/
				
				/*
				 * Mapping Version.
				 */
				User old_user = DataMap.getUserMap().get(user);
				User new_user = null;
				if(old_user != null){
					IoSession s = SessionMap.getMap().get(old_user.sessionId).getSession();
					if(s != null){
						s.close(false);
					}
					old_user = null;
				}
				new_user = new User();
				new_user.sessionId = session.getId();
				new_user.username = user;
				new_user.boundUuid = null;
				DataMap.getUserMap().put(user, new_user);
				holder.setType(SHolder.TYPE_PHONE);
				holder.isLogin = true;
				MLog.logger.debug("Phone Login Finished.");
				
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
				String user_to_reg = jsonMsg.getString("USER");

				/* for DB */
				/*
				List<UserTemp> list_01 = DBHelper.listUserBySsid(session
						.getId());
				if (list_01.size() > 0) {
					UserTemp userObj_01 = list_01.get(0);
					userObj_01.setBoundUuid(uuid_01);
					DBHelper.updateUser(userObj_01);
					MLog.logger.debug("Update reg. " + session.getId());
				}
				*/
				
				/*
				 * for Mapping
				 */
				User regUser = DataMap.getUserMap().get(user_to_reg);
				if(regUser.sessionId == session.getId()){
					Home home_to_reg = DataMap.getHomeMap().get(uuid_01);
					if(home_to_reg != null){
						//Home is online.
						regUser.boundUuid = uuid_01;
						home_to_reg.getUsers().add(user_to_reg);
						MLog.logger.debug("Phone Reg Finished.");
					}else{
						//Home is not alive.
					}
				}else{
					//The username is illegal, not equals to the SessionId.
				}
				break;

			/* 撤除注册的主控UUID，将不会推送，从MAP<UUID,SET<USER>>里删除 */
			case MCommon.MSG_PHONE_UNREG_UUID:
				MLog.logger.debug(" -- MSG_PHONE_UNREG_UUID -- ");
				String uuid_02 = jsonMsg.getString("UUID");
				String user_02 = jsonMsg.getString("USER");

				/* for DB */
				/*
				List<UserTemp> list_02 = DBHelper.listUserBySsid(session
						.getId());
				if (list_02.size() > 0) {
					UserTemp userObj_02 = list_02.get(0);
					userObj_02.setBoundUuid(null);
					DBHelper.updateUser(userObj_02);
					MLog.logger.debug("Update Un reg. " + session.getId());
				}
				*/
				
				/*
				 * for Mapping
				 */
				User regUser_02 = DataMap.getUserMap().get(user_02);
				if(regUser_02.sessionId == session.getId()){
					Home home_02 = DataMap.getHomeMap().get(regUser_02.boundUuid);
					if(home_02 != null){
						home_02.getUsers().remove(regUser_02);
					}
					regUser_02.boundUuid = null;
				}else{
					//The username is illegal, not equals to the SessionId.
				}
				
				break;

			/**********************************************
			 * Logic for Home;
			 **********************************************/
			/* 主控发过来的消息，进行UUID登记，存入MAP<UUID,SET<USER>>, 作为key */
			case MCommon.MSG_HOME_REG:
				MLog.logger.debug(" -- MSG_HOME_REG -- ");
				String homeUuid = jsonMsg.getString("UUID");

				/* register for DB */
				/*
				Long sid_old,sid_new;
				sid_new = session.getId();
				HomeTemp home_01 = new HomeTemp(homeUuid);
				home_01.setSessionId(sid_new);
				List<HomeTemp> list_home = DBHelper.listHomeByUuid(homeUuid);
				if (list_home.size() > 0) {
					HomeTemp home = list_home.get(0);
					sid_old = home.getSessionId();
					MLog.logger.debug("DB - home exist. " + sid_old);
					DBHelper.updateHome(home_01);	//更新数据库

					// 处理旧的连接  
					SessionMap.getMap().get(sid_old).getSession().close(false);
				} else {
					DBHelper.insertHome(home_01);
				}
				SessionMap.getMap().get(sid_new).setType(2);
				*/
				
				/*
				 * For Mapping
				 */
				Home old_home = DataMap.getHomeMap().get(homeUuid);
				Home new_home = null;
				if(old_home != null){
					IoSession s = SessionMap.getMap().get(old_home.sessionId).getSession();
					if(s != null){
						s.close(false);
					}
					old_home = null;
				}
				new_home = new Home();
				new_home.sessionId = session.getId();
				new_home.uuid = homeUuid;
				DataMap.getHomeMap().put(homeUuid, new_home);
				MLog.logger.debug("Home Reg Finished.");
				SessionMap.getMap().get(new_home.sessionId).setType(2);
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

				/*
				 * DB. version
				 */
				// get Users
				/*
				List<UserTemp> listToPush = DBHelper.listUserByUuid(toPushUuid);
				Map<Long, SHolder> map = SessionMap.getMap();
				for (UserTemp toPushUser : listToPush) {
					Long idToPush = toPushUser.getSessionId();
					SHolder holderToPush = map.get(idToPush);
					IoSession ssToPush = holderToPush.getSession();
					if (ssToPush == null) {
						continue;
					}
					// push Message
					byte[] toPushBytes = LogicUtils.packMsg(
							(short) MCommon.MSG_PUSH_ALARM, preBytes, preOff,
							preLen, bufBytes, off, len);
					IoBuffer mIoBuffer = IoBuffer.wrap(toPushBytes);
					ssToPush.write(mIoBuffer);
				}
				*/
				
				/*
				 * Mapping Version.
				 */
				Home home_to_push = DataMap.getHomeMap().get(toPushUuid);
				if(home_to_push == null){
					session.close(false);
				}
				Set<String> users = home_to_push.getUsers();
				for (Iterator iterator = users.iterator(); iterator.hasNext();) {
					String userStr = (String) iterator.next();
					User user_to_push = DataMap.getUserMap().get(userStr);
					if(user_to_push != null){
						IoSession ssToPush = SessionMap.getMap().get(user_to_push.sessionId).getSession();
						if (ssToPush == null) {
							continue;
						}
						// push Message
						byte[] toPushBytes = LogicUtils.packMsg(
								(short) MCommon.MSG_PUSH_ALARM, preBytes, preOff,
								preLen, bufBytes, off, len);
						IoBuffer mIoBuffer = IoBuffer.wrap(toPushBytes);
						ssToPush.write(mIoBuffer);
					}
				}				
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
//		cause.printStackTrace();
		session.close(true);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		/* for DB */
		/*
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
		*/
		
	}

}
