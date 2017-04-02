package mingzuozhibi.spider;

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

    @Autowired
    private AmazonDiscsSpider amazonDiscsSpider;

    @Scheduled(cron = "10 0/2 * * * ?")
    public void fetchSakuraSpeedData() {
        if (sakuraSpeedSpider.timeout()) {
            fetchSakuraSpeedData(3);
        }
    }

    @Scheduled(cron = "20 0/2 * * * ?")
    public void fetchAmazonDiscsData() {
        if (amazonDiscsSpider.timeout()) {
            fetchAmazonDiscsData(3);
        }
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
            }
            try {
                sakuraSpeedSpider.fetch();
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("fetching sakura speed data throw an error", e);
                }
                if (retry > 0) {
                    fetchSakuraSpeedData(retry - 1);
                } else {
                    return;
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("fetched sakura speed data ({})", retry);
            }
        }
    }

    private void fetchAmazonDiscsData(int retry) {
        Logger logger = LoggerFactory.getLogger(AutoRunSpider.class);
        if (retry == 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("fetching amazon discs data failed");
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("fetching amazon discs data ({})", retry);
            }
            try {
                amazonDiscsSpider.fetch();
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("fetching amazon discs data throw an error", e);
                }
                if (retry > 0) {
                    fetchSakuraSpeedData(retry - 1);
                } else {
                    return;
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("fetched amazon discs data ({})", retry);
            }
        }
    }

}
