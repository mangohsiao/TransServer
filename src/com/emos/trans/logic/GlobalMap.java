package com.emos.trans.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.emos.trans.MHolder;

public class GlobalMap {
	
	/**
	 * MAP<UUID,Set<USER> >
	 */
	private static Map<String, Set<String> > uidUserMap;
	
	/**
	 * MAP<USER, sessionID>
	 */
	private static Map<String, Long> userSsidMap;

	/**
	 * MAP<sessionID, MHolder>
	 */
	private static Map<Long, MHolder> ssidHolderMap;

	static{
		uidUserMap = new HashMap<String, Set<String>>();
		userSsidMap = new HashMap<String, Long>();
		ssidHolderMap = new HashMap<Long, MHolder>();
	}
	
	public static Map<String, Set<String>> getUidUserMap() {
		return uidUserMap;
	}

	public static Map<String, Long> getUserSsidMap() {
		return userSsidMap;
	}

	public static Map<Long, MHolder> getSsidHolderMap() {
		return ssidHolderMap;
	}
	
	
}
