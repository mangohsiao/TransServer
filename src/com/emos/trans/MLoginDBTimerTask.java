package com.emos.trans;

import java.util.TimerTask;

import org.apache.mina.core.session.IoSession;

import com.emos.trans.log.MLog;
import com.emos.trans.logic.GlobalMap;
import com.emos.trans.logic.SHolder;

public class MLoginDBTimerTask extends TimerTask {

	private SHolder holder;

	public MLoginDBTimerTask(SHolder holder) {
		super();
		this.holder = holder;
	}

	@Override
	public void run() {
		// TODO 此方法在connect后60秒内无登录则触发，断开连接并清除资源。
		if (holder.isLogin || holder.type == MCommon.MHOLDER_TYPE_HOME) {
			System.out.println("Login timer Pass.");
			return;
		}

		/* 未登录，清除资源 */
		IoSession session = holder.getSession();
		Long id = session.getId();
		session.close(false);
		MLog.logger.debug("session : " + id + " closed.");
	}

}
