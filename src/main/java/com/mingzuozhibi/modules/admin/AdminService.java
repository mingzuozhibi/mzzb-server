package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseKeys.Type;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.modules.core.MessageRepository;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.GroupService;
import com.mingzuozhibi.modules.record.*;
import com.mingzuozhibi.modules.user.RememberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@LoggerBind(Name.SERVER_CORE)
public class AdminService extends BaseSupport {

    @Autowired
    private GroupService groupService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RememberRepository rememberRepository;

    @Transactional
    public void deleteExpiredRemembers() {
        long count = rememberRepository.deleteByExpiredBefore(Instant.now());
        bind.info("[自动任务][清理自动登入][共%d个]".formatted(count));
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
        bind.info("[自动任务][转存昨日排名][共%d个]".formatted(records.size()));
    }

    @Transactional
    public void recordRankAndComputePt() {
        // +9 timezone and prev hour, so +1h -1h = +0h
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();
        Set<Disc> discs = groupService.findNeedRecordDiscs();
        discs.forEach(disc -> recordCompute.computePtNow(disc, date, hour));
        bind.info("[自动任务][记录计算排名][共%d个]".formatted(discs.size()));
    }

    @Transactional
    public void cleanupModulesMessages() {
        int count = 0;
        {
            int c1 = messageRepository.cleanup(Name.SPIDER_CONTENT, 600, Type.INFO, Type.WARNING);
            int c2 = messageRepository.cleanup(Name.SPIDER_CONTENT, 800);
            count += c1 + c2;
            log.info("[清理日志][name=%s][size=%d,%d]".formatted(Name.SPIDER_CONTENT, c1, c2));
        }
        {
            int c1 = messageRepository.cleanup(Name.SPIDER_HISTORY, 150, Type.INFO);
            int c2 = messageRepository.cleanup(Name.SPIDER_HISTORY, 200);
            count += c1 + c2;
            log.info("[清理日志][name=%s][size=%d,%d]".formatted(Name.SPIDER_HISTORY, c1, c2));
        }
        {
            int c1 = messageRepository.cleanup(Name.SERVER_DISC, 150, Type.INFO);
            int c2 = messageRepository.cleanup(Name.SERVER_DISC, 200);
            count += c1 + c2;
            log.info("[清理日志][name=%s][size=%d,%d]".formatted(Name.SERVER_DISC, c1, c2));
        }
        {
            int c1 = messageRepository.cleanup(Name.SERVER_CORE, 200);
            count += c1;
            log.info("[清理日志][name=%s][size=%d]".formatted(Name.SERVER_CORE, c1));
        }
        {
            int c1 = messageRepository.cleanup(Name.DEFAULT, 200);
            count += c1;
            log.info("[清理日志][name=%s][size=%d]".formatted(Name.DEFAULT, c1));
        }
        bind.info("[自动任务][清理日志][共%d条]".formatted(count));
    }

}
