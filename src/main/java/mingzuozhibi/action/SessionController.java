package mingzuozhibi.action;

import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.JsonArg;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class SessionController extends BaseController {

    @Autowired
    private Dao dao;

    @Autowired
    private UserDetailsService userDetailsService;

    @GetMapping(value = "/api/session", produces = CONTENT_TYPE)
    public String status() {
        LOGGER.debug("状态获取: 正在检测登入状态");

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            if (!"anonymousUser".equals(username)) {
                LOGGER.info("状态获取: 检测到已登入用户, username={}", username);
                JSONObject object = new JSONObject();
                object.put("success", true);
                object.put("username", username);
                putAuthority(object, authentication);
                return object.toString();
            } else {
                LOGGER.debug("状态获取: 检测到匿名用户");
                return booleanResult(false);
            }
        } else {
            LOGGER.debug("状态获取: 未检测到已登入状态");
            return booleanResult(false);
        }
    }

    private void putAuthority(JSONObject object, Authentication authentication) {
        authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .reduce((s1, s2) -> s1 + "," + s2)
                .ifPresent(roles -> object.put("roles", roles));
    }

    @PostMapping(value = "/api/session", produces = CONTENT_TYPE)
    public String login(@JsonArg("$.username") String username,
                        @JsonArg("$.password") String password) {
        LOGGER.debug("用户登入: username={}, password=******", username);

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails.getPassword().equals(password) && userDetails.isEnabled()) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                onLoginSuccess(username);
                LOGGER.info("用户登入: 用户已成功登入, username={}", username);
                return booleanResult(true);
            } else {
                LOGGER.debug("用户登入: 未能成功登入, username={}", username);
                return booleanResult(false);
            }
        } catch (UsernameNotFoundException ignored) {
            LOGGER.debug("用户登入: 未找到该用户, username={}", username);
            return booleanResult(false);
        }
    }

    private void onLoginSuccess(String username) {
        User user = dao.lookup(User.class, "username", username);
        user.setLastLoggedIn(LocalDateTime.now());
        dao.save(user);
    }

    @DeleteMapping(value = "/api/session", produces = CONTENT_TYPE)
    public String logout() {
        LOGGER.debug("用户登出: 正在检测登入状态");

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            context.setAuthentication(null);
            if (!"anonymousUser".equals(username)) {
                LOGGER.debug("用户登出: 用户已成功登出, username={}", username);
                return booleanResult(true);
            } else {
                LOGGER.debug("用户登出: 检测到匿名用户");
                return booleanResult(true);
            }
        } else {
            LOGGER.debug("用户登出: 未检测到已登入状态");
            return booleanResult(false);
        }
    }

}
