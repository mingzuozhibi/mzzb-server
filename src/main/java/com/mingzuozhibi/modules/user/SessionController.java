package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseController2;
import com.mingzuozhibi.commons.check.CheckResult;
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

import static com.mingzuozhibi.commons.check.CheckHelper.*;
import static com.mingzuozhibi.commons.check.CheckUtils.paramNoExists;

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
        } else if (!SessionUtils.isLogged(optional.get())) {
            String token = SessionUtils.getTokenFromHeader();
            sessionService.vaildSession(token).ifPresent(session -> {
                onSessionLogin(session.getUser(), false);
            });
        }
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

    @PostMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogin(@JsonArg("$.username") String username,
                               @JsonArg("$.password") String password) {
        CheckResult checks = runAllCheck(
            checkNotEmpty(username, "用户名称"),
            checkIdentifier(username, "用户名称", 4, 20),
            checkNotEmpty(password, "用户密码"),
            checkMd5Encode(password, "用户密码", 32)
        );
        if (checks.hasError()) {
            return errorResult(checks.getError());
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
        onSessionLogout();
        return buildSessionAndCount();
    }

    private void onSessionLogin(User user, boolean buildNew) {
        if (buildNew) {
            Session session = sessionService.buildSession(user);
            SessionUtils.setSessionId(session.getId());
            SessionUtils.setTokenToHeader(session.getToken());
        }
        setAuthentication(buildUserAuthentication(user));
        user.setLastLoggedIn(Instant.now());
    }

    private void onSessionLogout() {
        Long sessionId = SessionUtils.getSessionId();
        sessionService.cleanSession(sessionId);
        SessionUtils.setTokenToHeader("");
        setAuthentication(buildGuestAuthentication());
    }

}
