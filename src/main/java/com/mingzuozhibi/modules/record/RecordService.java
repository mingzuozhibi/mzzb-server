package com.mingzuozhibi.modules.record;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDate;

@Service
public class RecordService extends BaseSupport {

    @Autowired
    private HourRecordRepository hourRecordRepository;

    @Autowired
    private DateRecordRepository dateRecordRepository;

    @Transactional
    public HourRecord buildHourRecord(Disc disc, LocalDate date) {
        var hourRecords = findHourRecords(disc, date);
        if (hourRecords.isEmpty()) {
            return hourRecordRepository.save(new HourRecord(disc, date));
        }
        return hourRecords.get();
    }

    @Transactional
    public void moveRecord(HourRecord hourRecord, DateRecord dateRecord) {
        dateRecordRepository.save(dateRecord);
        hourRecordRepository.delete(hourRecord);
    }

    @Transactional
    public List<HourRecord> findHourRecords(LocalDate date) {
        return hourRecordRepository.findByDateBeforeOrderByDate(date);
    }

    @Transactional
    public List<DateRecord> findDateRecordsAsc(Disc disc) {
        return dateRecordRepository.queryBeforeAsc(disc, getPlusDays(disc));
    }

    @Transactional
    public List<DateRecord> findDateRecords(LocalDate date) {
        return dateRecordRepository.findByDate(date);
    }

    @Transactional
    public DateRecord findDateRecord(Disc disc, LocalDate date) {
        return dateRecordRepository.queryLastOne(disc, date);
    }

    @Transactional
    public JsonArray buildDiscRecords(Disc disc) {
        var array = new JsonArray();
        findHourRecords(disc, LocalDate.now()).ifPresent(record -> {
            array.add(buildRecord(record));
        });
        findDateRecordsDesc(disc).forEach(record -> {
            array.add(buildRecord(record));
        });
        return array;
    }

    private Optional<HourRecord> findHourRecords(Disc disc, LocalDate date) {
        return hourRecordRepository.findByDiscAndDate(disc, date);
    }

    private List<DateRecord> findDateRecordsDesc(Disc disc) {
        return dateRecordRepository.queryBeforeDesc(disc, getPlusDays(disc));
    }

    private LocalDate getPlusDays(Disc disc) {
        return disc.getReleaseDate().plusDays(7);
    }

    private JsonObject buildRecord(Record record) {
        var object = new JsonObject();
        object.addProperty("id", record.getId());
        object.addProperty("date", record.getDate().format(fmtDate));
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            if (rank < 10) {
                var doubleValue = BigDecimal.valueOf(rank)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
                object.addProperty("averRank", doubleValue);
            } else {
                object.addProperty("averRank", rank.intValue());
            }
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
