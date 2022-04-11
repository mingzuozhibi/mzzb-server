package com.mingzuozhibi.modules.record;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseService;
import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.mingzuozhibi.utils.FormatUtils.DATE_FORMATTER;

@Service
public class RecordService extends BaseService {

    @Autowired
    private HourRecordRepository hourRecordRepository;

    @Autowired
    private DateRecordRepository dateRecordRepository;

    @Transactional
    public HourRecord getOrCreateHourRecord(Disc disc, LocalDate date) {
        return hourRecordRepository.findByDiscAndDate(disc, date)
            .orElseGet(() -> hourRecordRepository.save(new HourRecord(disc, date)));
    }

    @Transactional
    public List<HourRecord> findHourRecords(LocalDate date) {
        return hourRecordRepository.findByDateBeforeOrderByDate(date);
    }

    @Transactional
    public DateRecord getDateRecord(Disc disc, LocalDate date) {
        return dateRecordRepository.getByDiscAndDate(disc, date);
    }

    @Transactional
    public DateRecord getLastDateRecord(Disc disc, LocalDate date) {
        return dateRecordRepository.getLastDateRecord(disc, date);
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
    public void moveRecord(HourRecord hourRecord, DateRecord dateRecord) {
        dateRecordRepository.save(dateRecord);
        hourRecordRepository.delete(hourRecord);
    }

    @Transactional
    public JsonArray buildRecords(Disc disc) {
        JsonArray array = new JsonArray();
        hourRecordRepository.findByDiscAndDate(disc, LocalDate.now())
            .ifPresent(record -> array.add(buildRecord(record)));
        dateRecordRepository.findDateRecords(disc, disc.getReleaseDate().plusDays(7))
            .forEach(record -> array.add(buildRecord(record)));
        return array;
    }

    public static JsonObject buildRecord(Record record) {
        JsonObject object = new JsonObject();
        object.addProperty("id", record.getId());
        object.addProperty("date", record.getDate().format(DATE_FORMATTER));
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            object.addProperty("averRank", rank.intValue());
        });
        Optional.ofNullable(record.getTodayPt()).ifPresent(todayPt -> {
            object.addProperty("todayPt", todayPt.intValue());
        });
        Optional.ofNullable(record.getTotalPt()).ifPresent(totalPt -> {
            object.addProperty("totalPt", totalPt.intValue());
        });
        Optional.ofNullable(record.getGuessPt()).ifPresent(guessPt -> {
            object.addProperty("guessPt", guessPt.intValue());
        });
        return object;
    }

}
