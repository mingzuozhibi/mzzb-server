package com.mingzuozhibi.config;

import com.allanditzel.springframework.security.web.csrf.CsrfTokenResponseHeaderBindingFilter;
import com.mingzuozhibi.modules.user.User;
import com.mingzuozhibi.modules.user.UserUitls;
import com.mingzuozhibi.security.CustomAccessDeniedHandler;
import com.mingzuozhibi.support.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CsrfFilter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private Dao dao;

    @Value("${mzzb.admin.password:mzzb1234}")
    private String adminPassword;

    protected void configure(HttpSecurity http) throws Exception {
        http

            .authorizeRequests()
            .antMatchers("/api/session/**").permitAll()
            .antMatchers(HttpMethod.GET).permitAll()
            .antMatchers("/api/**").hasRole("BASIC")

            .and().anonymous()
            .principal("Guest")
            .authorities("NONE")

            .and().exceptionHandling()
            .accessDeniedHandler(customAccessDeniedHandler)
            .authenticationEntryPoint(customAccessDeniedHandler)

            .and().csrf()
            .ignoringAntMatchers("/api/session/**")
            .ignoringAntMatchers("/management/**")

            .and().addFilterAfter(new CsrfTokenResponseHeaderBindingFilter(), CsrfFilter.class);

        Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
        logger.info("设置Security安全策略");

        dao.execute(session -> {
            String encode = UserUitls.encode("admin", adminPassword);
            User user = dao.lookup(User.class, "username", "admin");
            if (user == null) {
                user = new User("admin", encode, true);
                user.getRoles().add("ROLE_ADMIN");
                dao.save(user);
                logger.info("初始化管理员用户");
            } else {
                if (!encode.equals(user.getPassword())) {
                    logger.info("更新管理员密码");
                    user.setPassword(encode);
                }
                if (!user.getRoles().contains("ROLE_BASIC")) {
                    logger.info("更新管理员权限");
                    user.getRoles().add("ROLE_BASIC");
                }
                if (!user.getRoles().contains("ROLE_ADMIN")) {
                    logger.info("更新管理员权限");
                    user.getRoles().add("ROLE_ADMIN");
                }
                if (!user.isEnabled()) {
                    logger.info("启用管理员用户");
                    user.setEnabled(true);
                }
            }
        });
    }

}
