package com.emos.trans.msgInterface;

import java.util.HashMap;
import java.util.Map;

import com.emos.trans.log.MLog;
import com.emos.trans.logic.SHolder;

/**
 * @author mango
 *
 */
public class MsgDispatcher {
	
	/*
	 * 消息处理模块存储
	 */
	private static Map<Integer, IMsgHandler> handlerMap;
	
	/*
	 * 获取实例，单例模式
	 */
	public static Map<Integer, IMsgHandler> getIns(){
		if(handlerMap == null){
			handlerMap = new HashMap<Integer, IMsgHandler>();
		}
		return handlerMap;
	}
	
	/*
	 * 消息分发
	 */
	public static void processMsg(SHolder holder, int type, byte[] buffer, int len){
		//处理对应消息
		IMsgHandler handler = getIns().get(type);
		if(handler == null){
			//没有改消息处理模块
			MLog.logger.debug("没有消息类型为：0x" + Integer.toHexString(type) + "的处理模块");
			return;
		}
		
		handler.handleMsg(holder, type, buffer, len);
	}
	
	/*
	 * 注册处理模块
	 */
	public static void registerMsgHandler(int type, IMsgHandler handler) {
		getIns().put(type, handler);
		MLog.logger.info("注册消息处理模块：0x" + Integer.toHexString(type));
	}
}
