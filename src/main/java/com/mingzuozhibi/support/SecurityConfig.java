package com.mingzuozhibi.support;

import com.mingzuozhibi.modules.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CsrfFilter;

import static com.mingzuozhibi.utils.EncodeUtils.encodePassword;

@Slf4j
@EnableWebSecurity
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${security.password:unset}")
    private String adminPassword;

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityHandler securityHandler;

    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/api/session/**").permitAll()
            .antMatchers(HttpMethod.GET).permitAll()
            .antMatchers("/api/**").hasRole("BASIC")

            .and().anonymous()
            .principal("Guest")
            .authorities("NONE")

            .and().exceptionHandling()
            .accessDeniedHandler(securityHandler)
            .authenticationEntryPoint(securityHandler)

            .and().csrf()
            .ignoringAntMatchers("/api/session/**")
            .ignoringAntMatchers("/management/**")

            .and().addFilterAfter(new CsrfBindingFilter(), CsrfFilter.class);
        log.info("已成功配置安全策略");

        userService.initAdminUser(encodePassword("admin", adminPassword));
        log.info("已成功配置管理员用户");
    }

}
