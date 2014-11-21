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

public class PhoneLoginHandler extends MsgHandler {

	public int type = MCommon.MSG_PHONE_LOGIN;

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
			case MCommon.MSG_PHONE_LOGIN:
				MLog.logger.debug(" -- MSG_PHONE_LOGIN -- ");
				String user = jsonMsg.getString("USER");
				String pswd = jsonMsg.getString("PSWD");
				/* 检查该用户密码是否正确。 */
				if (TransClientLogic.checkUserPswd(user, pswd) < 0) {
					/* 登录失败 */
					return;
				}
				Long ssid_old,
				ssid_new;

				/* 检查该用户是否登陆过未清理，进行资源清理。或者通知下线。 */
				List<UserTemp> list = DBHelper.listUserByName(user);
				ssid_new = session.getId();
				UserTemp userNew = new UserTemp(user);
				userNew.setSessionId(ssid_new);
				if (list.size() > 0) {
					UserTemp usertemp = list.get(0);
					ssid_old = usertemp.getSessionId();
					MLog.logger.debug("DB - user exist. " + ssid_old);
					DBHelper.updateUser(userNew); // 更新数据库
					/* 处理旧的连接 */
					SessionMap.getMap().get(ssid_old).getSession().close(false);
				} else {
					// 插入user到DB表
					DBHelper.insertUser(userNew);
				}
				holder.setType(1);
				holder.isLogin = true;

				// TODO 发送确认消息给客户端。

				/* 登出，断开连接，清除User的资源及绑定关系 */
			case MCommon.MSG_PHONE_LOGOUT:
				MLog.logger.debug(" -- MSG_PHONE_LOGOUT -- ");
				/* 断开连接，清理旧的USER资源 */
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
