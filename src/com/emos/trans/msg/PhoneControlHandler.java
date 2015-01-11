package com.emos.trans.msg;

import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;

import com.emos.trans.MCommon;
import com.emos.trans.log.MLog;
import com.emos.trans.logic.LogicUtils;
import com.emos.trans.logic.SHolder;
import com.emos.trans.logic.SessionMap;
import com.emos.trans.msgInterface.MsgHandler;
import com.emos.trans.pojo.HomeTemp;
import com.emos.trans.util.DBHelper;

public class PhoneControlHandler extends MsgHandler {

	public int type = MCommon.MSG_PHONE_LOGIN;
	
	@Override
	public void handleMsg(SHolder holder, int type, byte[] buffer, int len) {
		// TODO Auto-generated method stub

		IoSession session = holder.session;

		// 创建字符串
		String strIn = new String(buffer, 0, len);
		MLog.logger.debug("PhoneControlHandler: " + strIn);
		try {
			JSONObject jsonMsg = new JSONObject(strIn);
			switch (type) {
			/* 主控发过来的消息，进行UUID登记，存入MAP<UUID,SET<USER>>, 作为key */
			case MCommon.MSG_PHONE_CONTROL:
				MLog.logger.debug("PhoneControlHandler： MSG_PHONE_CONTROL");
				String homeUuid = jsonMsg.getString("UUID");

				Long sid_home;
				/* list Home by uuid */
				List<HomeTemp> list_home = DBHelper.listHomeByUuid(homeUuid);
				if (list_home.size() > 0) {
					//if home is exist, then send the control message.
					HomeTemp home = list_home.get(0);
					sid_home = home.getSessionId();
					MLog.logger.debug("sending Msg to " + sid_home);
					// push Message, send bytes[]
					byte[] toPushBytes = LogicUtils.packMsg(
							(short) MCommon.MSG_PHONE_CONTROL_TRANS, buffer, 0,
							len, buffer, len, 0);
					IoBuffer mIoBuffer = IoBuffer.wrap(toPushBytes);
					SessionMap.getMap().get(sid_home).session.write(mIoBuffer);
				} else {
					//sendback Control Trans_re
					System.out.println("UUID :" + homeUuid + " NOT FOUND.");
				}
				break;

			/* 主控发过来的消息，进行UUID撤销，清除MAP<UUID,SET<USER>>中的key-value */
			case MCommon.MSG_PHONE_CONTROL_RE:
				MLog.logger.debug("PhoneControlHandler： MSG_PHONE_CONTROL_RE");
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
}
