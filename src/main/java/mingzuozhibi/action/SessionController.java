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
        JSONObject session = buildSession();

        if (!session.getBoolean("isLogged")) {
            String header = getAttributes().getRequest().getHeader("X-AUTO-LOGIN");
            if (header != null && header.length() == 36) {
                AutoLogin autoLogin = dao.lookup(AutoLogin.class, "token", header);
                String username = autoLogin.getUser().getUsername();

                if (autoLogin.getExpired().isAfter(LocalDateTime.now())) {
                    if (autoLogin.getUser().isEnabled()) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        doLoginSuccess(header, userDetails);

                        onLoginSuccess(username, true);



                        session = buildSession();

                        if (LOGGER.isInfoEnabled()) {
                            infoRequest("[用户自动登入: session={}]", session);
                        }

                        return objectResult(session);
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            debugRequest("[用户已被停用: username={}]", username);
                        }
                    }
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        debugRequest("[用户登入过期: username={}]", username);
                    }
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取当前登入状态: session={}]", session);
        }
        return objectResult(session);
    }

    @PostMapping(value = "/api/session/login", produces = MEDIA_TYPE)
    public String sessionLogin(
            @JsonArg("$.username") String username,
            @JsonArg("$.password") String password) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails.getPassword().equals(password)) {
                if (userDetails.isEnabled()) {

                    doLoginSuccess(password, userDetails);

                    onLoginSuccess(username, false);

                    JSONObject session = buildSession();
                    if (LOGGER.isInfoEnabled()) {
                        infoRequest("[用户成功登入: session={}]", session);
                    }
                    return objectResult(session);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        debugRequest("[用户已被停用: username={}]", username);
                    }
                    return errorMessage("用户已被停用");
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    debugRequest("[用户密码错误: username={}]", username);
                }
                return errorMessage("用户密码错误");
            }
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

    private void onLoginSuccess(String username, boolean isAutoLogin) {
        String header = UUID.randomUUID().toString();
        getAttributes().getResponse().addHeader("X-AUTO-LOGIN", header);

        dao.execute(session -> {
            User user = dao.lookup(User.class, "username", username);
            user.setLastLoggedIn(LocalDateTime.now().withNano(0));

            if (!isAutoLogin) {
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

        getAttributes().getRequest().getSession().invalidate();

        getAttributes().getResponse().addHeader("X-AUTO-LOGIN", "");

        dao.execute(session -> {
            User user = dao.lookup(User.class, "username", authentication.getName());
            dao.findBy(AutoLogin.class, "user", user).forEach(autoLogin -> {
                dao.delete(autoLogin);
            });
        });

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
