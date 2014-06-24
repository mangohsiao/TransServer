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
 * Home object for domain model class HomeTemp.
 * @see com.emos.trans.pojo.HomeTemp
 * @author Hibernate Tools
 */
public class HomeTempHome {

	private static final Log log = LogFactory.getLog(HomeTempHome.class);

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

	public void persist(HomeTemp transientInstance) {
		log.debug("persisting HomeTemp instance");
		try {
			sessionFactory.getCurrentSession().persist(transientInstance);
			log.debug("persist successful");
		} catch (RuntimeException re) {
			log.error("persist failed", re);
			throw re;
		}
	}

	public void attachDirty(HomeTemp instance) {
		log.debug("attaching dirty HomeTemp instance");
		try {
			sessionFactory.getCurrentSession().saveOrUpdate(instance);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void attachClean(HomeTemp instance) {
		log.debug("attaching clean HomeTemp instance");
		try {
			sessionFactory.getCurrentSession().lock(instance, LockMode.NONE);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void delete(HomeTemp persistentInstance) {
		log.debug("deleting HomeTemp instance");
		try {
			sessionFactory.getCurrentSession().delete(persistentInstance);
			log.debug("delete successful");
		} catch (RuntimeException re) {
			log.error("delete failed", re);
			throw re;
		}
	}

	public HomeTemp merge(HomeTemp detachedInstance) {
		log.debug("merging HomeTemp instance");
		try {
			HomeTemp result = (HomeTemp) sessionFactory.getCurrentSession()
					.merge(detachedInstance);
			log.debug("merge successful");
			return result;
		} catch (RuntimeException re) {
			log.error("merge failed", re);
			throw re;
		}
	}

	public HomeTemp findById(java.lang.String id) {
		log.debug("getting HomeTemp instance with id: " + id);
		try {
			HomeTemp instance = (HomeTemp) sessionFactory.getCurrentSession()
					.get("com.emos.trans.pojo.HomeTemp", id);
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

	public List<HomeTemp> findByExample(HomeTemp instance) {
		log.debug("finding HomeTemp instance by example");
		try {
			List<HomeTemp> results = (List<HomeTemp>) sessionFactory
					.getCurrentSession()
					.createCriteria("com.emos.trans.pojo.HomeTemp")
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
