package mingzuozhibi.config;

import mingzuozhibi.service.AmazonNewDiscSpider;
import mingzuozhibi.service.AmazonScheduler;
import mingzuozhibi.service.SakuraSpeedSpider;
import mingzuozhibi.service.ScheduleMission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AutoRunConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoRunConfig.class);

    @Autowired
    private AmazonScheduler scheduler;

    @Autowired
    private ScheduleMission scheduleMission;

    @Autowired
    private SakuraSpeedSpider sakuraSpeedSpider;

    @Autowired
    private AmazonNewDiscSpider amazonNewDiscSpider;

    @Value("${IS_JAPAN_SERVER}")
    private boolean isJapanServer;

    @Value("${JAPAN_SERVER_IP}")
    private String japanServerIp;

    /**
     * call by MzzbServerApplication
     */
    public void runOnStartupServer() {
        if (!isJapanServer) {
//            sakuraSpeedSpider.fetch(true);
            scheduleMission.removeExpiredDiscsFromList();
            scheduleMission.removeExpiredAutoLoginData();
            scheduleMission.recordDiscsRankAndComputePt();
        }
    }

    @Scheduled(cron = "0 2 * * * ?")
    public void runOnEveryHour() {
        if (!isJapanServer) {
            LOGGER.info("每小时任务开始");
            scheduleMission.removeExpiredDiscsFromList();
            scheduleMission.removeExpiredAutoLoginData();
            scheduleMission.recordDiscsRankAndComputePt();
            LOGGER.info("每小时任务完成");
        }
    }

    @Scheduled(cron = "0 12 4 * * ?")
    public void runOnEveryDate() {
        if (!isJapanServer) {
//            sakuraSpeedSpider.fetch(true);
            scheduleMission.updateDiscsTitleAndRelease();
        }
    }

//    @Scheduled(cron = "10 0/2 * * * ?")
//    public void fetchSakuraSpeedData() {
//        if (!isJapanServer) {
//            sakuraSpeedSpider.fetch(false);
//        }
//    }

    @Scheduled(cron = "10 1/2 * * * ?")
    public void fetchAmazonRankData() {
        if (!isJapanServer) {
            scheduler.fetchData();
        }
    }

    @Scheduled(cron = "0 0 5/6 * * ?")
    public void fetchNewDiscData() {
        if (isJapanServer) {
            amazonNewDiscSpider.fetch();
        }
    }

    @Scheduled(cron = "0 0,20 0/6 * * ?")
    public void fetchNewDiscDataFromJapan() {
        if (!isJapanServer) {
            amazonNewDiscSpider.fetchFromJapan(japanServerIp);
        }
    }

}
