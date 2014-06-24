package com.emos.trans.test;

import java.util.List;

import com.emos.trans.log.MLog;
import com.emos.trans.pojo.UserTemp;
import com.emos.trans.util.DBHelper;

public class Test {
	public static void testDB(){
//		DBHelper.cleanTables();
//		List<UserTemp> rs = DBHelper.listUserByUuid("'abcdefg'");
//		MLog.logger.debug("res = " + rs.size());
		UserTemp user = new UserTemp("mangogogogo");
		user.setSessionId(1234567L);
		user.setBoundUuid("jiaomamnaman111");
		DBHelper.insertUser(user);

//		DBHelper.deleteUser(new UserTemp("mangogogogo"));
//		DBHelper.deleteUser(user);
		DBHelper.listUserByName("loooo");
		
//		DBHelper.cleanTables();
	}
}
