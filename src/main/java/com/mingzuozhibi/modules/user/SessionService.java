package com.mingzuozhibi.modules.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionRepository2 sessionRepository2;

    public Optional<Session> vaildSession(String token) {
        if (token == null || token.length() != 36) {
            return Optional.empty();
        }
        Optional<Session> byToken = sessionRepository2.findByToken(token);
        if (!byToken.isPresent()) {
            return Optional.empty();
        }
        Session session = byToken.get();
        if (session.getExpired().isBefore(Instant.now())) {
            return Optional.empty();
        }
        if (!session.getUser().isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public Session buildSession(User user) {
        String token = UUID.randomUUID().toString();
        Instant expired = Instant.now().plusMillis(TimeUnit.DAYS.toMillis(14));
        Session session = new Session(user, token, expired);
        sessionRepository2.save(session);
        return session;
    }

    public void cleanSession(Long sessionId) {
        if (sessionId != null) {
            sessionRepository2.deleteById(sessionId);
        }
    }

    public int countSession() {
        String sql = "SELECT COUNT(*) FROM SPRING_SESSION";
        try {
            return Objects.requireNonNull(jdbcTemplate.query(sql, rs -> rs.next() ? rs.getInt(1) : -1));
        } catch (DataAccessException e) {
            return -2;
        } catch (RuntimeException e) {
            return -3;
        }
    }

}
