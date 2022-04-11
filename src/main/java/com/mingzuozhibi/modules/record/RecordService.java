package com.mingzuozhibi.modules.record;

import com.google.gson.JsonArray;
import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RecordService {

    @Autowired
    private HourRecordRepository hourRecordRepository;

    @Autowired
    private DateRecordRepository dateRecordRepository;

    @Transactional
    public JsonArray buildBaseRecords(Disc disc) {
        JsonArray array = new JsonArray();
        hourRecordRepository.findByDiscAndDate(disc, LocalDate.now())
            .ifPresent(record -> array.add(RecordUtils.buildRecord(record)));
        dateRecordRepository.findDateRecords(disc, disc.getReleaseDate().plusDays(7))
            .forEach(record -> array.add(RecordUtils.buildRecord(record)));
        return array;
    }

    @Transactional
    public DateRecord getDateRecord(Disc disc, LocalDate date) {
        return dateRecordRepository.findByDiscAndDate(disc, date);
    }

    @Transactional
    public List<DateRecord> findDateRecords(Disc disc) {
        return dateRecordRepository.findByDiscOrderByDate(disc);
    }

    @Transactional
    public List<DateRecord> findDateRecords(LocalDate date) {
        return dateRecordRepository.findByDate(date);
    }

    @Transactional
    public HourRecord getOrCreateHourRecord(Disc disc, LocalDate date) {
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

}
