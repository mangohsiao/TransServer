package com.emos.trans.logic;

import java.util.HashMap;
import java.util.Map;

public class SessionMap {

	private static Map<Long, SHolder> map;
	static{
		map = new HashMap<Long, SHolder>();
	}

	public static Map<Long, SHolder> getMap() {
		return map;
	}	
}
