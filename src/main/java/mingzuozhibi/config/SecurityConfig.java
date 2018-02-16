package mingzuozhibi.config;

import com.allanditzel.springframework.security.web.csrf.CsrfTokenResponseHeaderBindingFilter;
import mingzuozhibi.action.BaseController;
import mingzuozhibi.persist.User;
import mingzuozhibi.security.CustomAuthenticationFilter;
import mingzuozhibi.support.Dao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private Dao dao;

    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/basic/**").hasRole("BASIC")
                .antMatchers("/api/session").permitAll()
                .antMatchers(HttpMethod.GET).permitAll()
                .antMatchers("/api/**").hasRole("BASIC")
                .and()
                .formLogin()
                .and()
                .logout()
                .logoutUrl("/api/session/logout")
                .logoutSuccessHandler(new LogoutHandler())
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new AuthenticationFailureHandler());

        http.csrf()
                .ignoringAntMatchers("/api/session/**")
                .ignoringAntMatchers("/actuator/**", "/loggers/**", "/jolokia/**");

        http.addFilterAt(customAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.addFilterAfter(new CsrfTokenResponseHeaderBindingFilter(), CsrfFilter.class);

        Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
        logger.info("设置安全策略");
    }

    @Bean
    public CustomAuthenticationFilter customAuthenticationFilter() throws Exception {
        CustomAuthenticationFilter filter = new CustomAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(new LoginSuccessHandler());
        filter.setAuthenticationFailureHandler(new LoginFailureHandler());
        filter.setAuthenticationManager(authenticationManagerBean());
        filter.setFilterProcessesUrl("/api/session/login");
        return filter;
    }

    private class LoginSuccessHandler extends BaseController implements AuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
            dao.execute(session -> {
                String username = authentication.getName();
                LOGGER.info("用户成功登入，用户名为：{}", username);
                dao.lookup(User.class, "username", username)
                        .setLastLoggedIn(LocalDateTime.now().withNano(0));
            });
            response.getWriter().write(objectResult(getJSON(authentication)));
            response.flushBuffer();
        }
    }

    private class LoginFailureHandler extends BaseController implements org.springframework.security.web.authentication.AuthenticationFailureHandler {
        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
            response.getWriter().write(errorMessage(exception.getMessage()));
            response.flushBuffer();
        }
    }

    private class LogoutHandler extends BaseController implements LogoutSuccessHandler {
        @Override
        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            response.getWriter().write(objectResult(getJSON(null)));
            response.flushBuffer();
        }
    }

    private class AuthenticationFailureHandler extends BaseController implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            response.getWriter().write(errorMessage(authException.getMessage()));
            response.flushBuffer();
        }
    }


    @RestController
    public class SessionController extends BaseController {

        @GetMapping(value = "/api/session", produces = MEDIA_TYPE)
        public String current() {
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication authentication = context.getAuthentication();
            return objectResult(getJSON(authentication));
        }

    }

    private JSONObject getJSON(Authentication authentication) {
        JSONObject object = new JSONObject();

        if (authentication != null) {
            String name = authentication.getName();
            boolean isLogged = authentication.isAuthenticated() && !"anonymousUser".equals(name);
            if (isLogged) {
                object.put("userName", name);
                object.put("isLogged", true);
                object.put("userRoles", getUserRoles(authentication));
                return object;
            }
        }

        object.put("userName", "Guest");
        object.put("isLogged", false);
        object.put("userRoles", new JSONArray());
        return object;
    }

    private JSONArray getUserRoles(Authentication authentication) {
        JSONArray userRoles = new JSONArray();
        authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .forEach(userRoles::put);
        return userRoles;
    }

}
