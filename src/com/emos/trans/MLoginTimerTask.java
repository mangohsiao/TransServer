package com.emos.trans;

import java.util.TimerTask;

import com.emos.trans.logic.GlobalMap;

public class MLoginTimerTask extends TimerTask {

	private MHolder holder;

	public MLoginTimerTask(MHolder holder) {
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
		long id = holder.session.getId();
		holder.session.close(false);
		System.out.println(id + " closed.");
		GlobalMap.getSsidHolderMap().remove(id);
		holder = null;
		System.out.println(id + " clean.");
	}

}
