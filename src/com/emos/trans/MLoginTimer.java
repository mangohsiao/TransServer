package com.emos.trans;

import java.util.Timer;
import java.util.TimerTask;

/*
 * 起一个守护进程，负责清理 连接后未登录的。
 */
public class MLoginTimer {
	
	static {
		timer = new Timer();
	}
	
	private static Timer timer;
	
	public static void schedule(MLoginTimerTask task, long delay){
		timer.schedule(task, delay);
	}
	
	public static void cancel(){
		timer.cancel();
	}
	/* Login cleaning */
//	public 
}
