package mingzuozhibi.config;

import mingzuozhibi.service.EveryHourTask;
import mingzuozhibi.service.SakuraSpeedSpider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AutoRunConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoRunConfig.class);

    @Autowired
    private EveryHourTask everyHourTask;

    @Autowired
    private SakuraSpeedSpider sakuraSpeedSpider;

    /**
     * call by MzzbServerApplication
     */
    public void runStartupServer() {
        runEveryHourTask();
    }

    @Scheduled(cron = "10 0/2 * * * ?")
    public void fetchSakuraSpeedData() {
        fetchSakuraSpeedData(3);
    }

    @Scheduled(cron = "0 2 * * * ?")
    public void runEveryHourTask() {
        LOGGER.info("每小时任务开始");
        everyHourTask.run();
        LOGGER.info("每小时任务完成");
    }

    private void fetchSakuraSpeedData(int retry) {
        LOGGER.debug("正在更新sakura数据 (还有{}次机会)", retry);
        try {
            sakuraSpeedSpider.fetch();
            LOGGER.info("成功更新sakura数据");
        } catch (IOException e) {
            if (retry > 0) {
                LOGGER.debug("更新sakura数据时抛出一个错误", e);
                fetchSakuraSpeedData(retry - 1);
            } else {
                LOGGER.warn("更新sakura数据失败", e);
            }
        }
    }

}
