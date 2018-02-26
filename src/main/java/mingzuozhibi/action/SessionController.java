package mingzuozhibi.action;

import mingzuozhibi.persist.AutoLogin;
import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.JsonArg;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class SessionController extends BaseController {

    public static final Set<GrantedAuthority> GUEST_AUTHORITIES = Stream.of("NONE")
            .map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

    @Autowired
    private Dao dao;

    @Autowired
    private UserDetailsService userDetailsService;

    @GetMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String sessionQuery() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (!isLogged(authentication)) {
            if (checkAutoLogin()) {

                String username = authentication.getName();
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                doLoginSuccess(userDetails.getPassword(), userDetails);
                onLoginSuccess(username, false);

                if (LOGGER.isInfoEnabled()) {
                    infoRequest("[用户自动登入: 已成功自动登入][username={}]", username);
                }
            } else {
                getAttributes().getResponse().addHeader("X-AUTO-LOGIN", "");
            }
        }
        
        JSONObject session = buildSession();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取当前登入状态: session={}]", session);
        }
        return objectResult(session);
    }

    private boolean checkAutoLogin() {
        String header = getAttributes().getRequest().getHeader("X-AUTO-LOGIN");
        if (header == null || header.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                debugRequest("[用户自动登入: 未发现有效的X-AUTO-LOGIN]");
            }
            return false;
        }

        if (header.length() != 36) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[用户自动登入: 异常的X-AUTO-LOGIN][token={}]", header);
            }
            return false;
        }

        AutoLogin autoLogin = dao.lookup(AutoLogin.class, "token", header);
        if (autoLogin == null) {
            if (LOGGER.isDebugEnabled()) {
                debugRequest("[用户自动登入: 服务器未找到相应数据][token={}]", header);
            }
            return false;
        }

        String username = autoLogin.getUser().getUsername();
        if (autoLogin.getExpired().isBefore(LocalDateTime.now())) {
            if (LOGGER.isDebugEnabled()) {
                debugRequest("[用户自动登入: TOKEN已过期][username={}][expired={}]",
                        username, autoLogin.getExpired());
            }
            return false;
        }

        if (!autoLogin.getUser().isEnabled()) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[用户自动登入: 用户已被停用][username={}]", username);
            }
            return false;
        }

        return true;
    }

    @PostMapping(value = "/api/session/login", produces = MEDIA_TYPE)
    public String sessionLogin(
            @JsonArg("$.username") String username,
            @JsonArg("$.password") String password) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!userDetails.getPassword().equals(password)) {
                if (LOGGER.isDebugEnabled()) {
                    debugRequest("[用户密码错误: username={}]", username);
                }
                return errorMessage("用户密码错误");
            }

            if (!userDetails.isEnabled()) {
                if (LOGGER.isDebugEnabled()) {
                    debugRequest("[用户已被停用: username={}]", username);
                }
                return errorMessage("用户已被停用");
            }

            doLoginSuccess(password, userDetails);
            onLoginSuccess(username, true);

            JSONObject session = buildSession();
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[用户成功登入: session={}]", session);
            }
            return objectResult(session);

        } catch (UsernameNotFoundException e) {
            if (LOGGER.isDebugEnabled()) {
                debugRequest("[用户名称不存在: username={}]", username);
            }
            return errorMessage("用户名称不存在");
        }
    }

    private void doLoginSuccess(String password, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities());
        token.setDetails(new WebAuthenticationDetails(getAttributes().getRequest()));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(token);

        getAttributes().getRequest().changeSessionId();
    }

    private void onLoginSuccess(String username, boolean putAutoLoginToken) {
        dao.execute(session -> {
            User user = dao.lookup(User.class, "username", username);
            user.setLastLoggedIn(LocalDateTime.now().withNano(0));

            if (putAutoLoginToken) {
                String header = UUID.randomUUID().toString();
                getAttributes().getResponse().addHeader("X-AUTO-LOGIN", header);

                LocalDateTime expired = LocalDateTime.now().withNano(0).plusDays(14);
                dao.save(new AutoLogin(user, header, expired));
            }
        });
    }

    @PostMapping(value = "/api/session/logout", produces = MEDIA_TYPE)
    public String sessionLogout() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        context.setAuthentication(new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), "Guest", GUEST_AUTHORITIES)
        );

        dao.execute(session -> {
            User user = dao.lookup(User.class, "username", authentication.getName());
            dao.findBy(AutoLogin.class, "user", user).forEach(autoLogin -> {
                if (LOGGER.isDebugEnabled()) {
                    debugRequest("[用户登出: 删除自动登入数据][token={}]", autoLogin.getToken());
                }
                dao.delete(autoLogin);
            });
        });

        getAttributes().getResponse().addHeader("X-AUTO-LOGIN", "");

        getAttributes().getRequest().getSession().invalidate();

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[用户成功登出: username={}]", authentication.getName());
        }
        return objectResult(buildSession());
    }

    public static JSONObject buildSession() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        JSONObject object = new JSONObject();

        object.put("userName", authentication.getName());
        object.put("isLogged", isLogged(authentication));
        object.put("userRoles", getUserRoles(authentication));

        return object;
    }

    private static boolean isLogged(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Predicate.isEqual("ROLE_BASIC"));
    }

    public static JSONArray getUserRoles(Authentication authentication) {
        JSONArray userRoles = new JSONArray();
        authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .forEach(userRoles::put);
        return userRoles;
    }

}
