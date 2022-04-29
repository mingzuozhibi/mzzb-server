package com.mingzuozhibi;

import com.mingzuozhibi.commons.mylog.JmsMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@EnableScheduling
@EnableJdbcHttpSession
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
    JacksonAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
})
public class MzzbServerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
            SpringApplication.run(MzzbServerApplication.class, args);
        JmsMessage jmsMessage = context.getBean(JmsMessage.class);
        jmsMessage.notify("MzzbServerApplication已启动");
    }

}
