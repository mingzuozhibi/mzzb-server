package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseController;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

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
                Optional<Session> sessionOpt = sessionService.vaildSession();
                if (sessionOpt.isPresent()) {
                    onSessionLogin(sessionOpt.get().getUser(), false);
                } else {
                    sessionService.cleanSession();
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

    private static class LoginForm {
        String username;
        String password;

    }

    @PostMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionLogin(@RequestBody LoginForm form) {
        Optional<User> byUsername = userRepository.findByUsername(form.username);
        if (!byUsername.isPresent()) {
            return errorMessage("用户名称不存在");
        }
        User user = byUsername.get();
        if (!user.getPassword().equals(form.password)) {
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
        setAuthentication(buildUserAuthentication(user));
        user.setLastLoggedIn(LocalDateTime.now().withNano(0));
        if (buildNew) {
            Session session = sessionService.buildSession(user);
            SessionUtils.setTokenToHeader(session.getToken());
            SessionUtils.setSessionIdToHttpSession(session);
            getHttpRequest().changeSessionId();
        }
    }

    private void onSessionLogout() {
        setAuthentication(buildGuestAuthentication());
        sessionService.cleanSession();
        getHttpSession().invalidate();
    }

}
