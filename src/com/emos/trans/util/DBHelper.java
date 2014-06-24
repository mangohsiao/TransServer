package com.emos.trans.util;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import com.emos.trans.pojo.BaseDAO;
import com.emos.trans.pojo.HomeTemp;
import com.emos.trans.pojo.UserTemp;

public class DBHelper {
	private static BaseDAO<UserTemp> daoUser = new BaseDAO<UserTemp>();
	private static BaseDAO<HomeTemp> daoHome = new BaseDAO<HomeTemp>();

	/**
	 * operation for User
	 **/	
	/* insert user */
	public static void insertUser(UserTemp user){
		daoUser.create(user);
	}
	
	/* update user */
	public static void updateUser(UserTemp user){
		daoUser.update(user);
	}
	
	/* delete user */
	public static void deleteUser(UserTemp user){
		daoUser.delete(user);
	}

	/* list user */
	public static List<UserTemp> listUser(){
		return daoUser.list("FROM UserTemp");
	}
	
	/* list user by user_name */
	public static List<UserTemp> listUserByName(String user_name){
		return daoUser.list("FROM UserTemp as temp WHERE temp.userName = '" + user_name + "'");
	}
	
	/* list user by ssid */
	public static List<UserTemp> listUserBySsid(Long ssid){
		return daoUser.list("FROM UserTemp as temp WHERE temp.sessionId = '" + ssid + "'");
	}
	
	/**
	 * operation for Home
	 **/	
	/* insert home */
	public static void insertHome(HomeTemp home){
		daoHome.create(home);
	}
	/* update home */
	public static void updateHome(HomeTemp home){
		daoHome.update(home);
	}
	/* delete home */
	public static void deleteHome(HomeTemp home){
		daoHome.delete(home);
	}
	/* list home */
	public static List<HomeTemp> listHome(){
		return daoHome.list("FROM HomeTemp");
	}

	public static List<HomeTemp> listHomeByUuid(String homeUuid) {
		return daoHome.list("FROM HomeTemp as temp WHERE temp.homeUuid = '" + homeUuid + "'");
	}

	public static List<HomeTemp> listHomeBySsid(Long sid) {
		return daoHome.list("FROM HomeTemp as temp WHERE temp.sessionId = '" + sid + "'");
	}
	
	/* list bound user */
	public static List<UserTemp> listUserByUuid(String uuid){
		return daoUser.list("FROM UserTemp AS temp WHERE temp.boundUuid='" + uuid + "'");
	}

	/* clean all tables */
	public static void cleanTables(){
		Session session = HibernateUtil.getSessionFactory()
				.openSession();

		String hql_01 = "delete FROM UserTemp";
		String hql_02 = "delete FROM HomeTemp";
		try {
			session.beginTransaction();
			session.createQuery(hql_01).executeUpdate();
			session.createQuery(hql_02).executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			session.getTransaction().rollback();
		} finally {
			session.close();
		}
	}

}
