package com.emos.trans.logic;

import java.util.HashSet;
import java.util.Set;

public class Home {
	public String uuid;
	public Long sessionId;
	
	private Set<String> users;

	public Set<String> getUsers() {
		if(users == null){
			users = new HashSet<String>();
		}
		return users;
	}
	
}
