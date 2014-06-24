package com.emos.trans.pojo;

// Generated 2014-6-24 16:56:43 by Hibernate Tools 3.4.0.CR1

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import static org.hibernate.criterion.Example.create;

/**
 * Home object for domain model class UserTemp.
 * @see com.emos.trans.pojo.UserTemp
 * @author Hibernate Tools
 */
public class UserTempHome {

	private static final Log log = LogFactory.getLog(UserTempHome.class);

	private final SessionFactory sessionFactory = getSessionFactory();

	protected SessionFactory getSessionFactory() {
		try {
			return (SessionFactory) new InitialContext()
					.lookup("SessionFactory");
		} catch (Exception e) {
			log.error("Could not locate SessionFactory in JNDI", e);
			throw new IllegalStateException(
					"Could not locate SessionFactory in JNDI");
		}
	}

	public void persist(UserTemp transientInstance) {
		log.debug("persisting UserTemp instance");
		try {
			sessionFactory.getCurrentSession().persist(transientInstance);
			log.debug("persist successful");
		} catch (RuntimeException re) {
			log.error("persist failed", re);
			throw re;
		}
	}

	public void attachDirty(UserTemp instance) {
		log.debug("attaching dirty UserTemp instance");
		try {
			sessionFactory.getCurrentSession().saveOrUpdate(instance);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void attachClean(UserTemp instance) {
		log.debug("attaching clean UserTemp instance");
		try {
			sessionFactory.getCurrentSession().lock(instance, LockMode.NONE);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void delete(UserTemp persistentInstance) {
		log.debug("deleting UserTemp instance");
		try {
			sessionFactory.getCurrentSession().delete(persistentInstance);
			log.debug("delete successful");
		} catch (RuntimeException re) {
			log.error("delete failed", re);
			throw re;
		}
	}

	public UserTemp merge(UserTemp detachedInstance) {
		log.debug("merging UserTemp instance");
		try {
			UserTemp result = (UserTemp) sessionFactory.getCurrentSession()
					.merge(detachedInstance);
			log.debug("merge successful");
			return result;
		} catch (RuntimeException re) {
			log.error("merge failed", re);
			throw re;
		}
	}

	public UserTemp findById(java.lang.String id) {
		log.debug("getting UserTemp instance with id: " + id);
		try {
			UserTemp instance = (UserTemp) sessionFactory.getCurrentSession()
					.get("com.emos.trans.pojo.UserTemp", id);
			if (instance == null) {
				log.debug("get successful, no instance found");
			} else {
				log.debug("get successful, instance found");
			}
			return instance;
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}

	public List<UserTemp> findByExample(UserTemp instance) {
		log.debug("finding UserTemp instance by example");
		try {
			List<UserTemp> results = (List<UserTemp>) sessionFactory
					.getCurrentSession()
					.createCriteria("com.emos.trans.pojo.UserTemp")
					.add(create(instance)).list();
			log.debug("find by example successful, result size: "
					+ results.size());
			return results;
		} catch (RuntimeException re) {
			log.error("find by example failed", re);
			throw re;
		}
	}
}
