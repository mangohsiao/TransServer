package com.emos.trans.logic;

import java.util.HashMap;
import java.util.Map;

public class DataMap {
	
	/*
	 * UserName - User
	 */
	private static Map<String, User> userMap;
	
	/*
	 * Uuid - Home
	 */
	private static Map<String, Home> homeMap;

	public static Map<String, User> getUserMap() {
		return userMap;
	}

	public static Map<String, Home> getHomeMap() {
		return homeMap;
	}
	
	static{
		userMap = new HashMap<String, User>();
		homeMap = new HashMap<String, Home>();
	}
}
