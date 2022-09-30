package com.mingzuozhibi.configs;

import com.mingzuozhibi.modules.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

import static com.mingzuozhibi.support.EncodeUtils.encodePassword;

@Slf4j
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${security.password:admin}")
    private String adminPassword;

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityHandler securityHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers(HttpMethod.GET).permitAll()
            .antMatchers("/api/session/**").permitAll()
            .antMatchers("/api/**").hasRole("BASIC")

            .and().anonymous()
            .principal("Guest")
            .authorities("NONE")

            .and().exceptionHandling()
            .accessDeniedHandler(securityHandler)
            .authenticationEntryPoint(securityHandler)

            .and().csrf()
            .ignoringAntMatchers("/api/session/**")

            .and().addFilterAfter(new CsrfBindingFilter(), CsrfFilter.class);
        log.info("已成功配置安全策略");

        userService.initAdminUser(encodePassword("admin", adminPassword));
        log.info("已成功配置管理员用户");

        return http.build();
    }

}
