package mingzuozhibi.config;

import com.allanditzel.springframework.security.web.csrf.CsrfTokenResponseHeaderBindingFilter;
import mingzuozhibi.security.CustomAuthenticationEntryPoint;
import mingzuozhibi.security.CustomAuthenticationFilter;
import mingzuozhibi.security.CustomAuthenticationHandler;
import mingzuozhibi.security.CustomLogoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CustomLogoutHandler customLogoutHandler;

    @Autowired
    private CustomAuthenticationHandler customAuthenticationHandler;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;


    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/basic/**").hasRole("BASIC")
                .antMatchers("/api/session").permitAll()
                .antMatchers(HttpMethod.GET).permitAll()
                .antMatchers("/api/**").hasRole("BASIC");

        http.formLogin();

        http.logout()
                .logoutUrl("/api/session/logout")
                .addLogoutHandler(customLogoutHandler)
                .logoutSuccessHandler(customLogoutHandler);

        http.exceptionHandling()
                .accessDeniedHandler(customAuthenticationEntryPoint)
                .authenticationEntryPoint(customAuthenticationEntryPoint);

        http.csrf()
                .ignoringAntMatchers("/api/session/**")
                .ignoringAntMatchers("/actuator/**", "/loggers/**", "/jolokia/**");

        http.addFilterBefore(new AcceptHeaderLocaleFilter(), UsernamePasswordAuthenticationFilter.class);

        http.addFilterAt(customAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.addFilterAfter(new CsrfTokenResponseHeaderBindingFilter(), CsrfFilter.class);

        Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
        logger.info("设置安全策略");
    }

    private CustomAuthenticationFilter customAuthenticationFilter() throws Exception {
        CustomAuthenticationFilter filter = new CustomAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(customAuthenticationHandler);
        filter.setAuthenticationFailureHandler(customAuthenticationHandler);
        filter.setAuthenticationManager(authenticationManagerBean());
        filter.setFilterProcessesUrl("/api/session/login");
        return filter;
    }

    private static class AcceptHeaderLocaleFilter implements Filter {
        private AcceptHeaderLocaleResolver localeResolver;

        public AcceptHeaderLocaleFilter() {
            localeResolver = new AcceptHeaderLocaleResolver();
            localeResolver.setDefaultLocale(Locale.US);
        }

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            Locale locale = localeResolver.resolveLocale((HttpServletRequest) request);
            LocaleContextHolder.setLocale(locale);

            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    }
}
