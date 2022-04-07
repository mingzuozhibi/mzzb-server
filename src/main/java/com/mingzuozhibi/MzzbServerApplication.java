package com.mingzuozhibi;

import com.google.gson.Gson;
import com.mingzuozhibi.commons.gson.GsonFactory;
import com.mingzuozhibi.config.AutoRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@EnableScheduling
@EnableJdbcHttpSession
@SpringBootApplication
public class MzzbServerApplication {

    public static final Logger LOGGER = LoggerFactory.getLogger(MzzbServerApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MzzbServerApplication.class, args);
        LOGGER.info("MzzbServer服务已启动");
        context.getBean(AutoRunConfig.class).runOnStartupServer();
    }

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

}
