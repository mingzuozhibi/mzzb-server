package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.group.DiscGroupService;
import com.mingzuozhibi.modules.record.DateRecord;
import com.mingzuozhibi.modules.record.HourRecord;
import com.mingzuozhibi.modules.record.RecordService;
import com.mingzuozhibi.modules.record.RecordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static com.mingzuozhibi.modules.record.RecordUtils.computePt;
import static com.mingzuozhibi.modules.record.RecordUtils.safeIntValue;

@Slf4j
@Service
public class ComputeService {

    @Autowired
    private DiscGroupService discGroupService;

    @Autowired
    private RecordService recordService;

    @Transactional
    public void computeDisc(Disc disc) {
        recordService.findDateRecords(disc).forEach(record0 -> {
            LocalDate date = record0.getDate();
            DateRecord record1 = recordService.getDateRecord(disc, date.minusDays(1));
            DateRecord record7 = recordService.getDateRecord(disc, date.minusDays(7));
            RecordUtils.computePt(disc, date, record0, record1, record7);
        });
        if (LocalDate.now().isBefore(disc.getReleaseDate())) {
            computePtNow(disc);
        }
    }

    @Transactional
    public void computeDate(LocalDate date) {
        recordService.findDateRecords(date).forEach(record0 -> {
            Disc disc = record0.getDisc();
            DateRecord record1 = recordService.getDateRecord(disc, date.minusDays(1));
            DateRecord record7 = recordService.getDateRecord(disc, date.minusDays(7));
            RecordUtils.computePt(disc, date, record0, record1, record7);
        });
    }

    @Transactional
    public void computePtNow(Disc disc) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();
        computePtNow(disc, date, hour);
    }

    private void computePtNow(Disc disc, LocalDate date, int hour) {
        HourRecord record0 = recordService.getOrCreateHourRecord(disc, date);
        DateRecord record1 = recordService.getDateRecord(disc, date.minusDays(1));
        DateRecord record7 = recordService.getDateRecord(disc, date.minusDays(7));

        record0.setRank(hour, disc.getThisRank());
        computePt(disc, date, record0, record1, record7);

        disc.setTodayPt(safeIntValue(record0.getTodayPt()));
        disc.setTotalPt(safeIntValue(record0.getTotalPt()));
        disc.setGuessPt(safeIntValue(record0.getGuessPt()));
    }

    @Transactional
    public int recordRankAndComputePt() {
        // +9 timezone and prev hour, so +1h -1h = +0h
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();
        Set<Disc> discs = discGroupService.findNeedRecordDiscs();
        discs.forEach(disc -> computePtNow(disc, date, hour));
        return discs.size();
    }

}
