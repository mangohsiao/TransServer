package com.emos.trans.msg;

import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;

import com.emos.trans.MCommon;
import com.emos.trans.log.MLog;
import com.emos.trans.logic.LogicUtils;
import com.emos.trans.logic.SHolder;
import com.emos.trans.logic.SessionMap;
import com.emos.trans.logic.TransClientLogic;
import com.emos.trans.msgInterface.MsgHandler;
import com.emos.trans.pojo.HomeTemp;
import com.emos.trans.pojo.UserTemp;
import com.emos.trans.util.DBHelper;

public class HomeAlarmHandler extends MsgHandler {

	public int type = MCommon.MSG_HOME_ALARM;

	@Override
	public void handleMsg(SHolder holder, int type, byte[] buffer, int len) {

		IoSession session = holder.session;

		// 创建字符串
		String strIn = new String(buffer, 0, len);
		MLog.logger.debug("RegisterHandler: " + strIn);
		try {
			JSONObject jsonMsg = new JSONObject(strIn);
			MLog.logger.debug(" -- MSG_HOME_ALARM -- ");
			String toPushUuid = jsonMsg.getString("UUID");

			// get Users
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
						(short) MCommon.MSG_PUSH_ALARM, buffer, 0,
						len, buffer, len, 0);
				IoBuffer mIoBuffer = IoBuffer.wrap(toPushBytes);
				ssToPush.write(mIoBuffer);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
