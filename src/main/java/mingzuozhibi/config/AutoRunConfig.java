package mingzuozhibi.config;

import mingzuozhibi.service.AmazonNewDiscSpider;
import mingzuozhibi.service.AmazonScheduler;
import mingzuozhibi.service.ScheduleMission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Service
@RestController
public class AutoRunConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoRunConfig.class);

    @Autowired
    private AmazonScheduler scheduler;

    @Autowired
    private ScheduleMission scheduleMission;

    @Autowired
    private AmazonNewDiscSpider amazonNewDiscSpider;

    @Value("${JAPAN_SERVER_IP}")
    private String japanServerIp;

    /**
     * call by MzzbServerApplication
     */
    public void runOnStartupServer() {
        scheduleMission.removeExpiredDiscsFromList();
        scheduleMission.removeExpiredAutoLoginData();
        scheduleMission.recordDiscsRankAndComputePt();
    }

    @Scheduled(cron = "0 2 * * * ?")
    public void runOnEveryHour() {
        LOGGER.info("每小时任务开始");
        scheduleMission.removeExpiredDiscsFromList();
        scheduleMission.removeExpiredAutoLoginData();
        scheduleMission.recordDiscsRankAndComputePt();
        LOGGER.info("每小时任务完成");
    }

    @Scheduled(cron = "0 12 4 * * ?")
    public void runOnEveryDate() {
        scheduleMission.updateDiscsTitleAndRelease();
    }

    @Scheduled(cron = "10 1/2 * * * ?")
    public void fetchAmazonRankData() {
        scheduler.fetchData();
    }

    @GetMapping("/requestNewDiscs")
    public void fetchNewDiscsDataRequest() {
        fetchNewDiscDataFromJapan();
    }

    @Scheduled(cron = "0 0,20 0/6 * * ?")
    public void fetchNewDiscDataFromJapan() {
        amazonNewDiscSpider.fetchFromJapan(japanServerIp);
    }

}
