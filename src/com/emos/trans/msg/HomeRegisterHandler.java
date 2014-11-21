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
import com.emos.trans.pojo.HomeTemp;
import com.emos.trans.pojo.UserTemp;
import com.emos.trans.util.DBHelper;

public class HomeRegisterHandler extends MsgHandler {

	public int type = MCommon.MSG_HOME_REG;

	@Override
	public void handleMsg(SHolder holder, int type, byte[] buffer, int len) {

		IoSession session = holder.session;

		// 创建字符串
		String strIn = new String(buffer, 0, len);
		MLog.logger.debug("RegisterHandler: " + strIn);
		try {
			JSONObject jsonMsg = new JSONObject(strIn);
			switch (type) {
			/* 主控发过来的消息，进行UUID登记，存入MAP<UUID,SET<USER>>, 作为key */
			case MCommon.MSG_HOME_REG:
				MLog.logger.debug("HomeRegisterHandler： MSG_HOME_REG");
				String homeUuid = jsonMsg.getString("UUID");

				/* register for DB */
				Long sid_old,
				sid_new;
				sid_new = session.getId();
				HomeTemp home_01 = new HomeTemp(homeUuid);
				home_01.setSessionId(sid_new);
				List<HomeTemp> list_home = DBHelper.listHomeByUuid(homeUuid);
				if (list_home.size() > 0) {
					HomeTemp home = list_home.get(0);
					sid_old = home.getSessionId();
					MLog.logger.debug("DB - home exist. " + sid_old);
					DBHelper.updateHome(home_01); // 更新数据库

					/* 处理旧的连接 */
					SessionMap.getMap().get(sid_old).getSession().close(false);
				} else {
					DBHelper.insertHome(home_01);
				}
				SessionMap.getMap().get(sid_new).setType(2);
				break;

			/* 主控发过来的消息，进行UUID撤销，清除MAP<UUID,SET<USER>>中的key-value */
			case MCommon.MSG_HOME_UNREG:
				MLog.logger.debug("HomeRegisterHandler： MSG_HOME_UNREG");
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
