package com.emos.trans.logic;

import java.util.List;

public class TransClientLogic {

	/*
	 * Message for Phone
	 */

	/* Login Message */
	public static final int MSG_PHONE_LOGIN = 0x11;
	public static final int MSG_PHONE_LOGIN_RE = 0x12;
	
	/* Login Message */
	public static final int MSG_PHONE_LOGOUT = 0x13;
	public static final int MSG_PHONE_LOGOUT_RE = 0x14;
	
	/* Register UUID Message */
	public static final int MSG_PHONE_REG_UUID = 0x15;
	public static final int MSG_PHONE_REG_UUID_RE = 0x16;

	/* Un-Register UUID Message */
	public static final int MSG_PHONE_UNREG_UUID = 0x17;
	public static final int MSG_PHONE_UNREG_UUID_RE = 0x18;
	
	/*
	 * 检查用户名密码是否正确。
	 */
	public static int checkUserPswd(String user, String pswd) {
		System.out.println("user: " + user + "\tpswd: " + pswd);
		return 0;
	}

	/*
	 * 检查用户名密码是否正确。
	 */
	public static boolean userIsAlive(String user) {
		System.out.println("Check Alive user: " + user);
		return true;
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
