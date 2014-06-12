package com.emos.trans.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MLog {
	
	public static Logger logger = LogManager.getLogger();
	
	public static void info(String str){
		logger.info(str);
	}
}
