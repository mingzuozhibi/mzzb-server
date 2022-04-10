package com.mingzuozhibi.modules.auth;

import com.mingzuozhibi.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RememberRepository extends JpaRepository<Remember, Long> {

    void deleteByUser(User user);

    Optional<Remember> findByToken(String token);

}
