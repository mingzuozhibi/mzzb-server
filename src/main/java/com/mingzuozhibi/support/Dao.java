package com.mingzuozhibi.support;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.orm.hibernate5.HibernateCallback;

import java.util.List;
import java.util.function.Consumer;

public interface Dao {

    Long save(Object object);

    <T> T get(Class<T> klass, Long id);

    void refresh(Object object);

    void update(Object object);

    void delete(Object object);

    <T> List<T> findAll(Class<T> klass);

    <T> List<T> findBy(Class<T> klass, String name, Object value);

    <T> T lookup(Class<T> klass, String name, Object value);

    <T> T jdbc(ReturningWork<T> work);

    <T> T query(HibernateCallback<T> action);

    void execute(Consumer<Session> action);

    Criteria create(Class<?> klass);

    Session session();

}
