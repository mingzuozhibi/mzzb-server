package mingzuozhibi.security;

import mingzuozhibi.persist.core.User;
import mingzuozhibi.persist.core.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@PropertySource("file:config/setting.properties")
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDao userDao;

    @Autowired
    private Environment env;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        String password = env.getProperty("security.admin.password");

        User user = userDao.findByUsername("admin");
        if (user == null) {
            user = new User();
            user.setUsername("admin");
            user.setPassword(password);
            userDao.save(user);

            logger.info("创建管理员用户");
        } else if (!password.equals(user.getPassword())) {
            user.setPassword(password);
            userDao.save(user);

            logger.info("更新管理员密码");
        }

        auth.userDetailsService(userDao::findByUsername);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        http.authorizeRequests()
                .antMatchers(HttpMethod.GET).permitAll()
                .antMatchers(HttpMethod.POST, "/api/users").permitAll()
                .anyRequest().authenticated();
        http.httpBasic();
        http.csrf().disable();

        logger.info("设置安全策略");
    }

}
