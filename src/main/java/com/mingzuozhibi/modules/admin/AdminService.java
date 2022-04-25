package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.group.DiscGroupService;
import com.mingzuozhibi.modules.record.*;
import com.mingzuozhibi.modules.remember.RememberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Set;

@Service
public class AdminService extends BaseSupport {

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private DiscGroupService discGroupService;

    @Autowired
    private RememberRepository rememberRepository;

    @Transactional
    public void deleteExpiredRemembers() {
        long count = rememberRepository.deleteByExpiredBefore(Instant.now());
        jmsMessage.info("[自动任务][清理自动登入][共%d个]", count);
    }

    @Transactional
    public void moveExpiredHourRecords() {
        List<HourRecord> records = recordService.findHourRecords(LocalDate.now());
        records.forEach(hourRecord -> {
            DateRecord dateRecord = new DateRecord(hourRecord.getDisc(), hourRecord.getDate());
            dateRecord.setRank(hourRecord.getAverRank());
            dateRecord.setTodayPt(hourRecord.getTodayPt());
            dateRecord.setTotalPt(hourRecord.getTotalPt());
            dateRecord.setGuessPt(hourRecord.getGuessPt());
            recordService.moveRecord(hourRecord, dateRecord);
        });
        jmsMessage.info("[自动任务][转存昨日排名][共%d个]", records.size());
    }

    @Transactional
    public void recordRankAndComputePt() {
        // +9 timezone and prev hour, so +1h -1h = +0h
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();
        Set<Disc> discs = discGroupService.findNeedRecordDiscs();
        discs.forEach(disc -> recordCompute.computePtNow(disc, date, hour));
        jmsMessage.info("[自动任务][记录计算排名][共%d个]", discs.size());
    }

}