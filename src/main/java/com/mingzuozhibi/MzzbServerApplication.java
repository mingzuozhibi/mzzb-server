package com.mingzuozhibi;

import com.mingzuozhibi.commons.mylog.JmsMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@EnableScheduling
@EnableJdbcHttpSession
@SpringBootApplication
public class MzzbServerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
            SpringApplication.run(MzzbServerApplication.class, args);
        JmsMessage jmsMessage = context.getBean(JmsMessage.class);
        jmsMessage.notify("MzzbServerApplication已启动");
    }

}
