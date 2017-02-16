package mingzuozhibi.autorun;

import mingzuozhibi.autorun.spider.SakuraSpeedSpider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AutoRunSpider {

    @Autowired
    private SakuraSpeedSpider sakuraSpeedSpider;

    @Scheduled(cron = "0 0/2 * * * ?")
    public void fetchSakuraSpeedData() {
        fetchSakuraSpeedData(3);
    }

    private void fetchSakuraSpeedData(int retry) {
        Logger logger = LoggerFactory.getLogger(AutoRunSpider.class);
        if (retry == 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("fetching sakura speed data failed");
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("fetching sakura speed data ({})", retry);
                try {
                    sakuraSpeedSpider.fetch();
                } catch (IOException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("fetching sakura speed data throw an error", e);
                    }
                    fetchSakuraSpeedData(retry - 1);
                }
            }
        }
    }

}
