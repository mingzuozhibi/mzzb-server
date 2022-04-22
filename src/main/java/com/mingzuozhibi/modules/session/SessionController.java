package com.mingzuozhibi.modules.session;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.modules.remember.Remember;
import com.mingzuozhibi.modules.user.User;
import com.mingzuozhibi.modules.user.UserRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.modules.session.Authentications.*;
import static com.mingzuozhibi.modules.session.SessionUtils.*;
import static com.mingzuozhibi.utils.ChecksUtils.*;

@Slf4j
@RestController
public class SessionController extends BaseController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
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

    @Setter
    private static class LoginForm {
        private String username;
        private String password;
    }

    @Transactional
    @PostMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogin(@RequestBody LoginForm form) {
        Optional<String> checks = runChecks(
            checkNotEmpty(form.username, "用户名称"),
            checkStrMatch(form.username, "用户名称", "[A-Za-z0-9_]{4,20}"),
            checkNotEmpty(form.password, "用户密码"),
            checkStrMatch(form.password, "用户密码", "[0-9a-f]{32}")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        Optional<User> byUsername = userRepository.findByUsername(form.username);
        if (!byUsername.isPresent()) {
            return paramNotExists("用户名称");
        }
        User user = byUsername.get();
        if (!Objects.equals(user.getPassword(), form.password)) {
            return errorResult("用户密码错误");
        }
        if (!user.isEnabled()) {
            return errorResult("用户已被停用");
        }
        onSessionLogin(user, true);
        return buildSessionAndCount();
    }

    @Transactional
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
