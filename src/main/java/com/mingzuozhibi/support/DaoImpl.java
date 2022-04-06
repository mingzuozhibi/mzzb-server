package com.mingzuozhibi.support;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.criterion.Restrictions.eq;

@Repository
public class DaoImpl implements Dao {

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public Long save(Object object) {
        return (Long) session().save(object);
    }

    @Transactional
    public <T> T get(Class<T> klass, Long id) {
        return (T) session().get(klass, id);
    }

    @Transactional
    public void refresh(Object object) {
        session().refresh(object);
    }

    @Transactional
    public void update(Object object) {
        session().update(object);
    }

    @Transactional
    public void delete(Object object) {
        session().delete(object);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> klass) {
        return create(klass).list();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <T> List<T> findBy(Class<T> klass, String name, Object value) {
        return create(klass).add(eq(name, value)).list();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <T> T lookup(Class<T> klass, String name, Object value) {
        return (T) create(klass).add(eq(name, value)).uniqueResult();
    }

    @Transactional
    public <T> T jdbc(ReturningWork<T> work) {
        return session().doReturningWork(work);
    }

    @Transactional
    public <T> T query(HibernateCallback<T> action) {
        return action.doInHibernate(session());
    }

    @Transactional
    public void execute(Consumer<Session> action) {
        action.accept(session());
    }

    public Criteria create(Class<?> klass) {
        return session().createCriteria(klass);
    }

    public Session session() {
        return entityManager.unwrap(Session.class);
    }

}
