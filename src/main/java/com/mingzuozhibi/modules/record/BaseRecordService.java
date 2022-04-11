package com.mingzuozhibi.modules.record;

import com.google.gson.JsonArray;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.group.DiscGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.mingzuozhibi.utils.ReCompute.safeIntValue;

@Service
public class BaseRecordService {

    @Autowired
    private DiscGroupService discGroupService;

    @Autowired
    private HourRecordRepository hourRecordRepository;

    @Autowired
    private DateRecordRepository dateRecordRepository;

    @Transactional
    public JsonArray findRecords(Disc disc) {
        JsonArray array = new JsonArray();
        hourRecordRepository.findByDiscAndDate(disc, LocalDate.now())
            .ifPresent(record -> array.add(BaseRecordUtils.buildRecord(record)));
        dateRecordRepository.findDateRecords(disc, disc.getReleaseDate().plusDays(7))
            .forEach(record -> array.add(BaseRecordUtils.buildRecord(record)));
        return array;
    }

    @Transactional
    public DateRecord findDateRecord(Disc disc, LocalDate date) {
        return dateRecordRepository.findByDiscAndDate(disc, date);
    }

    @Transactional
    public HourRecord findOrCreateHourRecord(Disc disc, LocalDate date) {
        return hourRecordRepository.findByDiscAndDate(disc, date)
            .orElseGet(() -> hourRecordRepository.save(new HourRecord(disc, date)));
    }

    @Transactional
    public int moveExpiredHourRecords() {
        List<HourRecord> hourRecords = hourRecordRepository.findByDateBeforeOrderByDate(LocalDate.now());
        hourRecords.forEach(hourRecord -> {
            DateRecord dateRecord = new DateRecord(hourRecord.getDisc(), hourRecord.getDate());
            Optional.ofNullable(hourRecord.getAverRank()).ifPresent(dateRecord::setRank);
            Optional.ofNullable(hourRecord.getTodayPt()).ifPresent(dateRecord::setTodayPt);
            Optional.ofNullable(hourRecord.getTotalPt()).ifPresent(dateRecord::setTotalPt);
            Optional.ofNullable(hourRecord.getGuessPt()).ifPresent(dateRecord::setGuessPt);
            dateRecordRepository.save(dateRecord);
            hourRecordRepository.delete(hourRecord);
        });
        return hourRecords.size();
    }

    @Transactional
    public int recordRankAndComputePt() {
        // +9 timezone and prev hour, so +1h -1h = +0h
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();
        Set<Disc> discs = discGroupService.findNeedRecordDiscs();
        discs.forEach(disc -> {
            HourRecord record0 = findOrCreateHourRecord(disc, date);
            BaseRecord record1 = findDateRecord(disc, date.minusDays(1));
            BaseRecord record7 = findDateRecord(disc, date.minusDays(7));

            record0.setRank(hour, disc.getThisRank());
            BaseRecordUtils.computePt(disc, date, record0, record1, record7);

            disc.setTodayPt(safeIntValue(record0.getTodayPt()));
            disc.setTotalPt(safeIntValue(record0.getTotalPt()));
            disc.setGuessPt(safeIntValue(record0.getGuessPt()));
        });
        return discs.size();
    }

}
