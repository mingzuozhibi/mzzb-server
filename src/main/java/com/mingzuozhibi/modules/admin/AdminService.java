package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseService;
import com.mingzuozhibi.modules.record.BaseRecordService;
import com.mingzuozhibi.modules.remember.RememberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService extends BaseService {

    @Autowired
    private BaseRecordService baseRecordService;

    @Autowired
    private RememberRepository rememberRepository;

    @Transactional
    public void deleteExpiredRemembers() {
        long count = rememberRepository.deleteExpiredRemembers();
        jmsMessage.info("[自动任务][清理自动登入][共%d个]", count);
    }

    @Transactional
    public void moveExpiredHourRecords() {
        int count = baseRecordService.moveExpiredHourRecords();
        jmsMessage.info("[自动任务][转存昨日排名][共%d个]", count);
    }

    @Transactional
    public void recordRankAndComputePt() {
        int count = baseRecordService.recordRankAndComputePt();
        jmsMessage.info("[自动任务][记录计算排名][共%d个]", count);
    }

}
