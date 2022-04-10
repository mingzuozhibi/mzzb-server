package com.mingzuozhibi.modules.auth;

import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.support.JsonArg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.ChecksUtils.*;
import static com.mingzuozhibi.commons.utils.SecurityUtils.*;
import static com.mingzuozhibi.modules.auth.SessionUtils.*;

@Slf4j
@RestController
public class SessionController extends BaseController2 {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionQuery() {
        Optional<Authentication> optional = getAuthentication();
        if (!optional.isPresent()) {
            log.debug("sessionQuery: Authentication is null");
            setAuthentication(buildGuestAuthentication());
        } else if (!isLogged(optional.get())) {
            String token = getTokenFromHeader();
            sessionService.vaildSession(token).ifPresent(remember -> {
                onSessionLogin(remember.getUser(), false);
            });
        }
        return buildSessionAndCount();
    }

    @PostMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogin(@JsonArg("$.username") String username,
                               @JsonArg("$.password") String password) {
        Optional<String> checks = runChecks(
            checkNotEmpty(username, "用户名称"),
            checkIdentifier(username, "用户名称", 4, 20),
            checkNotEmpty(password, "用户密码"),
            checkMd5Encode(password, "用户密码", 32)
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        Optional<User> byUsername = userRepository.findByUsername(username);
        if (!byUsername.isPresent()) {
            return paramNoExists("用户名称");
        }
        User user = byUsername.get();
        if (!Objects.equals(user.getPassword(), password)) {
            return errorResult("用户密码错误");
        }
        if (!user.isEnabled()) {
            return errorResult("用户已被停用");
        }
        onSessionLogin(user, true);
        return buildSessionAndCount();
    }

    @DeleteMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogout() {
        Long sessionId = getSessionId();
        sessionService.cleanSession(sessionId);
        setTokenToHeader("");
        setAuthentication(buildGuestAuthentication());
        return buildSessionAndCount();
    }

    private String buildSessionAndCount() {
        int userCount = sessionService.countSession();
        Optional<Authentication> optional = getAuthentication();
        if (optional.isPresent()) {
            return dataResult(new SessionAndCount(optional.get(), userCount));
        } else {
            return dataResult(new SessionAndCount(buildGuestAuthentication(), userCount));
        }
    }

    private void onSessionLogin(User user, boolean buildNew) {
        if (buildNew) {
            Remember remember = sessionService.buildSession(user);
            setSessionId(remember.getId());
            setTokenToHeader(remember.getToken());
        }
        setAuthentication(buildUserAuthentication(user));
        user.setLastLoggedIn(Instant.now());
    }

}
