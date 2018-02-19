package mingzuozhibi;

import mingzuozhibi.config.AutoRunConfig;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

import javax.persistence.EntityManagerFactory;

@EnableScheduling
@EnableJdbcHttpSession
@SpringBootApplication
public class MzzbServerApplication {

    public static final Logger LOGGER = LoggerFactory.getLogger(MzzbServerApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MzzbServerApplication.class, args);
        LOGGER.info("MzzbServer服务已启动");
        context.getBean(AutoRunConfig.class).runStartupServer();
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SessionFactory sessionFactory(EntityManagerFactory emf) {
        LOGGER.info("EntityManagerFactory已成功注入");
        return emf.unwrap(SessionFactory.class);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:org/springframework/security/messages");

        ReloadableResourceBundleMessageSource messageSourceLocal = new ReloadableResourceBundleMessageSource();
        messageSourceLocal.setBasename("classpath:messages");
        messageSourceLocal.setParentMessageSource(messageSource);

        return messageSourceLocal;
    }

}
