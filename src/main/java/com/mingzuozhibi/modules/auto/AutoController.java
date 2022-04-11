package com.mingzuozhibi.modules.auto;

import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import com.mingzuozhibi.modules.record.BaseRecordService;
import com.mingzuozhibi.modules.remember.RememberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutoController extends BaseController2 {

    @Autowired
    private BaseRecordService baseRecordService;

    @Autowired
    private RememberRepository rememberRepository;

    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping("/admin/runAutoTask")
    public void runAutoTask() {
        ThreadUtils.startThread(() -> {
            jmsMessage.info("自动任务开始");
            deleteExpiredRemembers();
            moveExpiredHourRecords();
            recordRankAndComputePt();
            jmsMessage.info("自动任务完成");
        });
    }

    private void deleteExpiredRemembers() {
        long count = rememberRepository.deleteExpiredRemembers();
        jmsMessage.info("[自动任务][清理自动登入][共%d个]", count);
    }

    private void moveExpiredHourRecords() {
        int count = baseRecordService.moveExpiredHourRecords();
        jmsMessage.info("[自动任务][转存昨日排名][共%d个]", count);
    }

    private void recordRankAndComputePt() {
        int count = baseRecordService.recordRankAndComputePt();
        jmsMessage.info("[自动任务][记录计算排名][共%d个]", count);
    }

}
