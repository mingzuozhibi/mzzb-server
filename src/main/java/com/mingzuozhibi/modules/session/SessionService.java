package com.mingzuozhibi.modules.session;

import com.mingzuozhibi.modules.remember.Remember;
import com.mingzuozhibi.modules.remember.RememberRepository;
import com.mingzuozhibi.modules.user.User;
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
    private RememberRepository rememberRepository;

    public Optional<Remember> vaildSession(String token) {
        if (token == null || token.length() != 36) {
            return Optional.empty();
        }
        Optional<Remember> byToken = rememberRepository.findByToken(token);
        if (!byToken.isPresent()) {
            SessionUtils.setTokenToHeader("");
            return Optional.empty();
        }
        Remember remember = byToken.get();
        if (remember.getExpired().isBefore(Instant.now())) {
            rememberRepository.delete(remember);
            return Optional.empty();
        }
        if (!remember.getUser().isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(remember);
    }

    public Remember buildSession(User user) {
        String token = UUID.randomUUID().toString();
        Instant expired = Instant.now().plusMillis(TimeUnit.DAYS.toMillis(14));
        Remember remember = new Remember(user, token, expired);
        rememberRepository.save(remember);
        return remember;
    }

    public void cleanSession(Long sessionId) {
        if (sessionId != null) {
            rememberRepository.deleteById(sessionId);
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
