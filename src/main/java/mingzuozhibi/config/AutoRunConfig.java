package mingzuozhibi.config;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.service.AmazonDiscSpider;
import mingzuozhibi.service.AmazonNewDiscSpider;
import mingzuozhibi.service.ScheduleMission;
import mingzuozhibi.support.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

@Service
@RestController
public class AutoRunConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoRunConfig.class);

    @Autowired
    private ScheduleMission scheduleMission;

    @Autowired
    private AmazonDiscSpider amazonDiscSpider;

    @Autowired
    private AmazonNewDiscSpider amazonNewDiscSpider;

    @Value("${BCLOUD_IP}")
    private String japanServerIp;

    /**
     * call by MzzbServerApplication
     */
    public void runOnStartupServer() {
        scheduleMission.removeExpiredAutoLoginData();
        scheduleMission.recordDiscsRankAndComputePt();
    }

    @Scheduled(cron = "0 2 * * * ?")
    public void runOnEveryHour() {
        LOGGER.info("每小时任务开始");
        scheduleMission.removeExpiredAutoLoginData();
        scheduleMission.recordDiscsRankAndComputePt();
        LOGGER.info("每小时任务完成");
    }

    @GetMapping("/requestNewDiscs")
    @Scheduled(cron = "0 0,20 0/6 * * ?")
    public void fetchNewDiscs() {
        amazonNewDiscSpider.fetchFromJapan(japanServerIp);
    }

    @GetMapping("/requestDiscRanks")
    @Scheduled(cron = "0 2/5 * * * ?")
    public void fetchDiscRanks() {
        new Thread(() -> {
            amazonDiscSpider.fetchFromBCloud();
        }).start();
    }

}
