package com.mingzuozhibi.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository2 extends JpaRepository<Session, Long> {

    void deleteByUser(User user);

}
