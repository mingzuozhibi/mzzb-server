package com.mingzuozhibi.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository2 extends JpaRepository<Session, Long> {

    void deleteByUser(User user);

    Optional<Session> findByToken(String token);

}
