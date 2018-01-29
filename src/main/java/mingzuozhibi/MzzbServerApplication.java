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

    private static Logger logger = LoggerFactory.getLogger(MzzbServerApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MzzbServerApplication.class, args);
        logger.info("服务已启动");
        context.getBean(AutoRunConfig.class).runStartupServer();
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SessionFactory sessionFactory(EntityManagerFactory emf) {
        return emf.unwrap(SessionFactory.class);
    }

}
