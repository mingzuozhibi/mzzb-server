package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseController2;
import com.mingzuozhibi.commons.check.CheckResult;
import com.mingzuozhibi.support.JsonArg;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mingzuozhibi.commons.check.CheckHelper.*;
import static com.mingzuozhibi.commons.check.CheckUtils.paramNoExists;

@RestController
public class SessionController extends BaseController2 {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SessionAndCount {
        private String userName;
        private Set<String> roleList;
        private boolean hasBasic;
        private boolean hasAdmin;
        private int sessionCount;

        public SessionAndCount(Authentication authentication, int sessionCount) {
            this.userName = authentication.getName();
            this.roleList = getRoleList(authentication);
            this.hasBasic = roleList.contains("ROLE_BASIC");
            this.hasAdmin = roleList.contains("ROLE_ADMIN");
            this.sessionCount = sessionCount;
        }

        private Set<String> getRoleList(Authentication authentication) {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        }
    }

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/api/session")
    public String sessionQuery() {
        getAuthentication().ifPresent(authentication -> {
            if (!SessionUtils.isLogged(authentication)) {
                String token = SessionUtils.getTokenFromHeader();
                Optional<Session> sessionOpt = sessionService.vaildSession(token);
                if (sessionOpt.isPresent()) {
                    onSessionLogin(sessionOpt.get().getUser(), false);
                } else {
                    onSessionLogout();
                }
            }
        });
        return buildSessionAndCount();
    }

    private String buildSessionAndCount() {
        int sessionCount = sessionService.countSession();
        Optional<Authentication> optional = getAuthentication();
        if (optional.isPresent()) {
            return dataResult(new SessionAndCount(optional.get(), sessionCount));
        } else {
            return dataResult(new SessionAndCount(buildGuestAuthentication(), sessionCount));
        }
    }

    @PostMapping("/api/session")
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
        if (!user.getPassword().equals(password)) {
            return errorResult("用户密码错误");
        }
        if (!user.isEnabled()) {
            return errorResult("用户已被停用");
        }
        onSessionLogin(user, true);
        return buildSessionAndCount();
    }

    @DeleteMapping("/api/session")
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
        getHttpSession().invalidate();
        setAuthentication(buildGuestAuthentication());
    }

}
