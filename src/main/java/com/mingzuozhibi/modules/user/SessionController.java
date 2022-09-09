package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.base.BaseController;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime2;
import static com.mingzuozhibi.commons.utils.LoggerUtils.findRealIp;
import static com.mingzuozhibi.modules.user.SessionUtils.*;
import static com.mingzuozhibi.support.ChecksUtils.*;
import static com.mingzuozhibi.support.FileIoUtils.writeLine;

@Slf4j
@RestController
public class SessionController extends BaseController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/session/current", produces = MEDIA_TYPE)
    public String sessionCurrent() {
        Optional<Authentication> optional = SessionUtils.getAuthentication();
        if (optional.isPresent()) {
            String name = optional.get().getName();
            Optional<User> byUsername = userRepository.findByUsername(name);
            if (byUsername.isPresent()) {
                return dataResult(byUsername.get());
            }
        }
        return errorResult("登入状态异常");
    }

    @Transactional
    @GetMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionQuery() {
        Optional<Authentication> optional = getAuthentication();
        if (optional.isEmpty()) {
            log.debug("sessionQuery: Authentication is null");
            setAuthentication(buildGuestAuthentication());
        } else {
            if (!isLogged(optional.get())) {
                String token = getSessionTokenFromHeader();
                sessionService.vaildSession(token).ifPresent(remember -> {
                    onSessionLogin(remember.getUser(), false);
                });
            }
        }
        return buildSessionAndCount();
    }

    private boolean isLogged(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(Predicate.isEqual("ROLE_BASIC"));
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
        if (byUsername.isEmpty()) {
            return paramNotExists("用户名称");
        }
        User user = byUsername.get();
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
                LocalDateTime.now().format(fmtDateTime2), realIp, username));
        });
    }

    @Transactional
    @DeleteMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogout() {
        Long sessionId = getSessionIdFromHttpSession();
        sessionService.cleanSession(sessionId);
        setSessionTokenToHeader("");
        setAuthentication(buildGuestAuthentication());
        return buildSessionAndCount();
    }

    private String buildSessionAndCount() {
        Long count = sessionService.countSession();
        Optional<Authentication> optional = getAuthentication();
        if (optional.isPresent()) {
            return dataResult(new SessionAndCount(optional.get(), count));
        } else {
            return dataResult(new SessionAndCount(buildGuestAuthentication(), count));
        }
    }

    private void onSessionLogin(User user, boolean buildNew) {
        if (buildNew) {
            Remember remember = sessionService.buildSession(user);
            setSessionIdToHttpSession(remember.getId());
            setSessionTokenToHeader(remember.getToken());
        }
        setAuthentication(buildUserAuthentication(user));
        user.setLastLoggedIn(Instant.now());
    }

}
