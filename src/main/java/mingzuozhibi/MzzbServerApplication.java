package mingzuozhibi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MzzbServerApplication {

    private static Logger logger = LoggerFactory.getLogger(MzzbServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MzzbServerApplication.class, args);
        logger.info("服务已启动");
    }

}
