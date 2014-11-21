package com.emos.trans.msg;

import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;

import com.emos.trans.MCommon;
import com.emos.trans.log.MLog;
import com.emos.trans.logic.SHolder;
import com.emos.trans.logic.SessionMap;
import com.emos.trans.logic.TransClientLogic;
import com.emos.trans.msgInterface.MsgHandler;
import com.emos.trans.pojo.UserTemp;
import com.emos.trans.util.DBHelper;

public class PhoneUUIDHandler extends MsgHandler {

	public int type = MCommon.MSG_PHONE_REG_UUID;

	@Override
	public void handleMsg(SHolder holder, int type, byte[] buffer, int len) {

		/* 登录验证，不通过则更新schedule=60秒 */

		IoSession session = holder.session;

		// 创建字符串
		String strIn = new String(buffer, 0, len);
		MLog.logger.debug("RegisterHandler: " + strIn);
		try {
			JSONObject jsonMsg = new JSONObject(strIn);
			switch (type) {

			/* 注册启动的安防主控的UUID */
			case MCommon.MSG_PHONE_REG_UUID:
				MLog.logger.debug(" -- MSG_PHONE_REG_UUID -- ");
				/* 查询是否注册？是的话删除之前的，否则加直接加入uuid-user */
				String uuid_01 = jsonMsg.getString("UUID");
				String user_01 = jsonMsg.getString("USER");

				/* for DB */
				List<UserTemp> list_01 = DBHelper.listUserBySsid(session
						.getId());
				if (list_01.size() > 0) {
					UserTemp userObj_01 = list_01.get(0);
					userObj_01.setBoundUuid(uuid_01);
					DBHelper.updateUser(userObj_01);
					MLog.logger.debug("Update reg. " + session.getId());
				}
				break;

			/* 撤除注册的主控UUID，将不会推送，从MAP<UUID,SET<USER>>里删除 */
			case MCommon.MSG_PHONE_UNREG_UUID:
				MLog.logger.debug(" -- MSG_PHONE_UNREG_UUID -- ");
				String uuid_02 = jsonMsg.getString("UUID");
				String user_02 = jsonMsg.getString("USER");

				/* for DB */
				List<UserTemp> list_02 = DBHelper.listUserBySsid(session
						.getId());
				if (list_02.size() > 0) {
					UserTemp userObj_02 = list_02.get(0);
					userObj_02.setBoundUuid(null);
					DBHelper.updateUser(userObj_02);
					MLog.logger.debug("Update Un reg. " + session.getId());
				}
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
