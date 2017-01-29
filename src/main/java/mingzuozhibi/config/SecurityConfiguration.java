package mingzuozhibi.config;

import mingzuozhibi.persist.core.User;
import mingzuozhibi.persist.core.UserDao;
import mingzuozhibi.support.JsonArg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@PropertySource("file:config/setting.properties")
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Autowired
    private UserDao userDao;

    @Value("${security.admin.password}")
    private String securityAdminPassword;

    @Value("${security.admin.userlist}")
    private String securityAdminUserlist;

    private MyUserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        setupAdminUser();
        userDetailsService = new MyUserDetailsService();
        auth.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/api/users/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET).permitAll()
                .antMatchers("/api/login").permitAll()
                .antMatchers("/api/**").hasRole("USER");
        http.httpBasic();
        http.csrf().disable();

        logger.info("设置安全策略");
    }

    @PostMapping("/api/login")
    public String ajaxLogin(@JsonArg("$.username") String username,
                            @JsonArg("$.password") String password) {
        logger.info("用户登入: username={}, password=******", username);
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails.getPassword().equals(password) && userDetails.isEnabled()) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, userDetails.getPassword(), userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("用户登入: 用户已成功登入, username={}", username);
                return "{\"success\": true}";
            } else {
                logger.info("用户登入: 未能成功登入, username={}", username);
            }
        } catch (UsernameNotFoundException ignored) {
            logger.info("用户登入: 未找到该用户, username={}", username);
        }
        return "{\"success\": false}";
    }

    private void setupAdminUser() {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        User user = userDao.findByUsername("admin");
        if (user == null) {
            user = new User();
            user.setUsername("admin");
            user.setPassword(securityAdminPassword);
            userDao.save(user);

            logger.info("创建管理员用户");
        } else if (!securityAdminPassword.equals(user.getPassword())) {
            user.setPassword(securityAdminPassword);
            userDao.save(user);

            logger.info("更新管理员密码");
        }
    }


    private class MyUserDetailsService implements UserDetailsService {

        private final List<GrantedAuthority> ADMIN_ROLES;
        private final List<GrantedAuthority> USER_ROLES;
        private final HashSet<String> ADMIN_LIST;

        private MyUserDetailsService() {
            ADMIN_ROLES = Stream.of("ROLE_USER", "ROLE_ADMIN")
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            USER_ROLES = Stream.of("ROLE_USER")
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            ADMIN_LIST = Stream.of(securityAdminUserlist.split(","))
                    .collect(Collectors.toCollection(HashSet::new));
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            User user = userDao.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("username " + username + " not found");
            }
            return new UserDetails() {
                public List<GrantedAuthority> getAuthorities() {
                    onLoginSuccess();
                    return ADMIN_LIST.contains(username) ? ADMIN_ROLES : USER_ROLES;
                }

                private void onLoginSuccess() {
                    user.setLastLoggedin(new Date());
                    userDao.save(user);
                }

                public String getPassword() {
                    return user.getPassword();
                }

                public String getUsername() {
                    return user.getUsername();
                }

                public boolean isEnabled() {
                    return user.isEnabled();
                }

                public boolean isAccountNonLocked() {
                    return true;
                }

                public boolean isAccountNonExpired() {
                    return true;
                }

                public boolean isCredentialsNonExpired() {
                    return true;
                }
            };
        }

    }
}
