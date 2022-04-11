package com.mingzuozhibi.modules.auto;

import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoRunner {

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private AutoService autoService;

    @Scheduled(cron = "0 0 * * * ?")
    public void runOnEveryHour() {
        ThreadUtils.startThread(() -> {
            jmsMessage.info("自动任务开始");
            autoService.deleteExpiredRemembers();
            autoService.moveExpiredHourRecords();
            autoService.recordRankAndComputePt();
            jmsMessage.info("自动任务完成");
        });
    }

}
