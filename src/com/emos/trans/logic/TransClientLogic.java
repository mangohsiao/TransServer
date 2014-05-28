package com.emos.trans.logic;

import java.util.List;

public class TransClientLogic {
	/*
	 * 检查用户名密码是否正确。
	 */
	public static int checkUserPswd(String user, String pswd){
		return 0;
	}
	
	/*
	 * put (user,sessionID) into MAP<USER, sessionID>
	 */
	public static int putUserSessionID() {
		return 0;
	}
	
	/*
	 * put (uuid, user) into MAP<UUID,List<USER> >
	 */
	public static int putUuidUser() {
		return 0;
	}
	
	/*
	 * get (User) List from Map<UUID,List<USER> >
	 */
	public static List<String> getUserByUuid() {
		return null;
	}
	
	/*
	 * get (sessionId) from Map<USER, sessionID>
	 */
	public static long getSsidByUser() {
		return 0;
	}
	
	/*
	 * 
	 */
}
