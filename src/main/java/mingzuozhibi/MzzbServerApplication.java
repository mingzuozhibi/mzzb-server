package mingzuozhibi;

import mingzuozhibi.config.AutoRunConfig;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.persistence.EntityManagerFactory;

@EnableScheduling
@SpringBootApplication
public class MzzbServerApplication {

    public static final Logger LOGGER = LoggerFactory.getLogger(MzzbServerApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MzzbServerApplication.class, args);
        LOGGER.info("MzzbServer服务已启动");
        context.getBean(AutoRunConfig.class).runOnStartupServer();
    }

    @Bean
    public SessionFactory sessionFactory(EntityManagerFactory emf) {
        LOGGER.info("EntityManagerFactory已成功注入");
        return emf.unwrap(SessionFactory.class);
    }

}
