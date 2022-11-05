package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.base.BaseController;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.mingzuozhibi.commons.base.BaseController.DEFAULT_TYPE;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.sdk.RealIpUtils.findRealIp;
import static com.mingzuozhibi.modules.user.SessionUtils.*;
import static com.mingzuozhibi.support.ChecksUtils.*;
import static com.mingzuozhibi.support.FileIoUtils.writeLine;

@Slf4j
@Transactional
@RestController
@RequestMapping(produces = DEFAULT_TYPE)
public class SessionController extends BaseController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasRole('BASIC')")
    @GetMapping("/api/session/current")
    public String sessionCurrent() {
        var optional = findAuthentication();
        if (optional.isPresent()) {
            var name = optional.get().getName();
            var byUsername = userRepository.findByUsername(name);
            if (byUsername.isPresent()) {
                return dataResult(byUsername.get());
            }
        }
        return errorResult("登入状态异常");
    }

    @GetMapping("/api/session")
    public String sessionQuery() {
        var optional = findAuthentication();
        if (optional.isEmpty()) {
            log.debug("sessionQuery: Authentication is null");
            setAuthentication(buildGuestAuthentication());
        } else if (!isLogged(optional.get())) {
            var token = getSessionTokenFromHeader();
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

    @PostMapping("/api/session")
    public String sessionLogin(@RequestBody LoginForm form) {
        var checks = runChecks(
            checkNotEmpty(form.username, "用户名称"),
            checkStrMatch(form.username, "用户名称", "[A-Za-z0-9_]{4,20}"),
            checkNotEmpty(form.password, "用户密码"),
            checkStrMatch(form.password, "用户密码", "[0-9a-f]{32}")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        var byUsername = userRepository.findByUsername(form.username);
        if (byUsername.isEmpty()) {
            return paramNotExists("用户名称");
        }
        var user = byUsername.get();
        if (!Objects.equals(user.getPassword(), form.password)) {
            logLoginFailed(form.username);
            return errorResult("用户密码错误");
        }
        if (!user.isEnabled()) {
            return errorResult("用户已被停用");
        }
        onSessionLogin(user, true);
        return buildSessionAndCount();
    }

    private void logLoginFailed(String username) {
        findRealIp().ifPresent(realIp -> {
            writeLine("var/ban.log", "%s %s %s".formatted(
                LocalDateTime.now().format(fmtDateTime), realIp, username));
        });
    }

    @DeleteMapping("/api/session")
    public String sessionLogout() {
        var sessionId = getSessionIdFromHttpSession();
        sessionService.cleanSession(sessionId);
        setSessionTokenToHeader("");
        setAuthentication(buildGuestAuthentication());
        return buildSessionAndCount();
    }

    private String buildSessionAndCount() {
        var count = sessionService.countSession();
        var optional = findAuthentication();
        if (optional.isPresent()) {
            return dataResult(new SessionAndCount(optional.get(), count));
        } else {
            return dataResult(new SessionAndCount(buildGuestAuthentication(), count));
        }
    }

    private void onSessionLogin(User user, boolean buildNew) {
        if (buildNew) {
            var remember = sessionService.buildSession(user);
            setSessionIdToHttpSession(remember.getId());
            setSessionTokenToHeader(remember.getToken());
        }
        setAuthentication(buildUserAuthentication(user));
        user.setLastLoggedIn(Instant.now());
    }

}
