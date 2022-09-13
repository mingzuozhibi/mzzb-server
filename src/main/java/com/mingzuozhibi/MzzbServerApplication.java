package com.mingzuozhibi;

import com.mingzuozhibi.commons.amqp.AmqpSender;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@EnableScheduling
@EnableJdbcHttpSession
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
    JacksonAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class MzzbServerApplication {

    public static void main(String[] args) {
        var context =
            SpringApplication.run(MzzbServerApplication.class, args);
        context.getBean(AmqpSender.class).bind(Name.SERVER_CORE)
            .notify("MzzbServerApplication已启动");
    }

}
