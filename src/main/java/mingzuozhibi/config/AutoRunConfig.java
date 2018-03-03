package mingzuozhibi.config;

import mingzuozhibi.service.HourlyMission;
import mingzuozhibi.service.SakuraSpeedSpider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Service
public class AutoRunConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoRunConfig.class);

    @Autowired
    private HourlyMission hourlyMission;

    @Autowired
    private SakuraSpeedSpider sakuraSpeedSpider;

    /**
     * call by MzzbServerApplication
     */
    public void runStartupServer() {
        fetchSakuraSpeedData(3);
        runEveryHourTask();
    }

    @Scheduled(cron = "10 0/2 * * * ?")
    public void fetchSakuraSpeedData() {
        fetchSakuraSpeedData(3);
    }

    @Scheduled(cron = "0 2 * * * ?")
    public void runEveryHourTask() {
        LOGGER.info("每小时任务开始");
        hourlyMission.doMission();
        LOGGER.info("每小时任务完成");
    }

    public void fetchSakuraSpeedData(int retry) {
        LOGGER.debug("正在更新Sakura(Speed)数据 (还有{}次机会)", retry);
        try {
            sakuraSpeedSpider.fetch();
            LOGGER.debug("成功更新Sakura(Speed)数据");
        } catch (SocketTimeoutException e) {
            if (retry > 0) {
                LOGGER.debug("抓取Sakura(Speed)数据超时，正在进行重试");
                fetchSakuraSpeedData(retry - 1);
            } else {
                LOGGER.warn("抓取超时, 更新Sakura(Speed)数据失败");
            }
        } catch (IOException e) {
            LOGGER.warn("抓取Sakura(Speed)数据遇到意外错误, Message={}", e.getMessage());
            LOGGER.debug("抓取Sakura(Speed)数据遇到意外错误", e);
        }
    }

}
