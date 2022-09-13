package com.mingzuozhibi.modules.record;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.modules.disc.Disc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class RecordCompute extends BaseSupport {

    @Autowired
    private RecordService recordService;

    @Transactional
    public void computeDisc(Disc disc) {
        var records = recordService.findDateRecordsAsc(disc);
        var size = records.size();
        log.debug("[计算碟片][共%d个][%s]".formatted(size, disc.getLogName()));
        records.forEach(record0 -> {
            var date = record0.getDate();
            var record1 = recordService.findDateRecord(disc, date.minusDays(1));
            var record7 = recordService.findDateRecord(disc, date.minusDays(7));
            computePt(disc, date, record0, record1, record7);
        });
        var now = LocalDateTime.now();
        var date = now.toLocalDate();
        if (date.isBefore(disc.getReleaseDate())) {
            computePtNow(disc, date, now.getHour());
        } else if (size > 0) {
            updateDiscPt(disc, records.get(size - 1));
        }
    }

    @Transactional
    public void computeDate(LocalDate date) {
        var records = recordService.findDateRecords(date);
        log.debug("[计算碟片][共%d个]".formatted(records.size()));
        records.forEach(record0 -> {
            var disc = record0.getDisc();
            var record1 = recordService.findDateRecord(disc, date.minusDays(1));
            var record7 = recordService.findDateRecord(disc, date.minusDays(7));
            computePt(disc, date, record0, record1, record7);
        });
    }

    @Transactional
    public void computePtNow(Disc disc, LocalDate date, int hour) {
        var record0 = recordService.buildHourRecord(disc, date);
        var record1 = recordService.findDateRecord(disc, date.minusDays(1));
        var record7 = recordService.findDateRecord(disc, date.minusDays(7));

        record0.setRank(hour, disc.getThisRank());
        computePt(disc, date, record0, record1, record7);
        updateDiscPt(disc, record0);
    }

    private static void updateDiscPt(Disc disc, Record record) {
        disc.setTodayPt(safeIntValue(record.getTodayPt()));
        disc.setTotalPt(safeIntValue(record.getTotalPt()));
        disc.setGuessPt(safeIntValue(record.getGuessPt()));
    }

    public static void computePt(Disc disc, LocalDate date, Record record0, Record record1, Record record7) {
        if (date.isBefore(disc.getReleaseDate())) {
            computeTodayPt(record0);
            computeTotalPt(record0, record1);
            computeGuessPt(record0, record7);
        } else if (record1 != null) {
            record0.setTodayPt(null);
            record0.setTotalPt(record1.getTotalPt());
            record0.setGuessPt(record1.getGuessPt());
        }
    }

    private static void computeTodayPt(Record record) {
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            record.setTodayPt(computeHourPt(record.getDisc(), rank));
        });
    }

    private static void computeTotalPt(Record record0, Record record1) {
        if (record1 == null || record1.getTotalPt() == null) {
            record0.setTotalPt(record0.getTodayPt());
        } else if (record0.getTodayPt() != null) {
            record0.setTotalPt(record0.getTodayPt() + record1.getTotalPt());
        } else {
            record0.setTotalPt(record1.getTotalPt());
        }
    }

    private static void computeGuessPt(Record record0, Record record7) {
        if (record7 == null) {
            return;
        }
        if (record0.getTotalPt() != null && record7.getTotalPt() != null) {
            var addPt = (record0.getTotalPt() - record7.getTotalPt()) / 7d;
            var releaseDate = record0.getDisc().getReleaseDate();
            var currentDate = record0.getDate();
            var days = releaseDate.toEpochDay() - currentDate.toEpochDay() - 1;
            record0.setGuessPt(record0.getTotalPt() + addPt * days);
        }
    }

    private static double computeHourPt(Disc disc, double rank) {
        return switch (disc.getDiscType()) {
            case Cd -> computeHourPt(3600, 5.25, rank);
            case Auto, Bluray -> computePtOfBD(rank);
            case Dvd -> computeHourPt(2400, 4.2, rank);
            default -> 0d;
        };
    }

    private static double computePtOfBD(double rank) {
        if (rank <= 10) {
            return computeHourPt(2400, 3.2, rank);
        } else if (rank <= 20) {
            return computeHourPt(2400, 3.3, rank);
        } else if (rank <= 50) {
            return computeHourPt(2400, 3.4, rank);
        } else if (rank <= 100) {
            return computeHourPt(2400, 3.6, rank);
        } else if (rank <= 300) {
            return computeHourPt(2400, 3.8, rank);
        } else {
            return computeHourPt(2400, 3.9, rank);
        }
    }

    private static double computeHourPt(int div, double base, double rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

    private static Integer safeIntValue(Double value) {
        return Optional.ofNullable(value).map(Double::intValue).orElse(null);
    }

}
