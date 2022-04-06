package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseController;
import com.mingzuozhibi.commons.check.CheckResult;
import com.mingzuozhibi.support.JsonArg;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.mingzuozhibi.commons.check.CheckHelper.*;
import static com.mingzuozhibi.commons.check.CheckUtils.paramNoExists;

@RestController
public class SessionController extends BaseController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping(value = "/api/session", produces = MEDIA_TYPE)
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
        return objectResult(buildSession());
    }

    public JSONObject buildSession() {
        JSONObject object = SessionUtils.buildSession();
        object.put("onlineUserCount", sessionService.countSession());
        return object;
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
            return errorMessage(checks.getError());
        }
        Optional<User> byUsername = userRepository.findByUsername(username);
        if (!byUsername.isPresent()) {
            return paramNoExists("用户名称");
        }
        User user = byUsername.get();
        if (!user.getPassword().equals(password)) {
            return errorMessage("用户密码错误");
        }
        if (!user.isEnabled()) {
            return errorMessage("用户已被停用");
        }
        onSessionLogin(user, true);
        return objectResult(buildSession());
    }

    @DeleteMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogout() {
        onSessionLogout();
        return objectResult(buildSession());
    }

    private void onSessionLogin(User user, boolean buildNew) {
        if (buildNew) {
            Session session = sessionService.buildSession(user);
            SessionUtils.setSessionId(session.getId());
            SessionUtils.setTokenToHeader(session.getToken());
        }
        setAuthentication(buildUserAuthentication(user));
        user.setLastLoggedIn(LocalDateTime.now().withNano(0));
    }

    private void onSessionLogout() {
        Long sessionId = SessionUtils.getSessionId();
        sessionService.cleanSession(sessionId);
        SessionUtils.setTokenToHeader("");
        getHttpSession().invalidate();
        setAuthentication(buildGuestAuthentication());
    }

}
