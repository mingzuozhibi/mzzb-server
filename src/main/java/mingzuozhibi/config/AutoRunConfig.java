package mingzuozhibi.config;

import mingzuozhibi.service.ScheduleMission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Service
@RestController
public class AutoRunConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoRunConfig.class);

    @Autowired
    private ScheduleMission scheduleMission;

    /**
     * call by MzzbServerApplication
     */
    public void runOnStartupServer() {
        new Thread(() -> {
            LOGGER.info("开机任务开始");
            scheduleMission.removeExpiredAutoLoginData();
            scheduleMission.moveHourRecordToDateRecord();
            scheduleMission.recordDiscsRankAndComputePt();
            LOGGER.info("开机任务完成");
        }).start();
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void runOnEveryHour() {
        new Thread(() -> {
            LOGGER.info("每小时任务开始");
            scheduleMission.removeExpiredAutoLoginData();
            scheduleMission.moveHourRecordToDateRecord();
            scheduleMission.recordDiscsRankAndComputePt();
            LOGGER.info("每小时任务完成");
        }).start();
    }

}
