package com.mingzuozhibi.modules.record;

import com.mingzuozhibi.commons.base.BaseService;
import com.mingzuozhibi.modules.disc.Disc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RecordCompute extends BaseService {

    @Autowired
    private RecordService recordService;

    @Transactional
    public void computeDisc(Disc disc) {
        List<DateRecord> records = recordService.findDateRecords(disc);
        int size = records.size();
        log.debug("[计算碟片][共{}个][{}]", size, disc.getLogName());
        records.forEach(record0 -> {
            LocalDate date = record0.getDate();
            DateRecord record1 = recordService.getDateRecord(disc, date.minusDays(1));
            DateRecord record7 = recordService.getDateRecord(disc, date.minusDays(7));
            computePt(disc, date, record0, record1, record7);
        });
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        if (date.isBefore(disc.getReleaseDate())) {
            computePtNow(disc, date, now.getHour());
        } else if (size > 0) {
            updateDiscPt(disc, records.get(size - 1));
        }
    }

    @Transactional
    public void computeDate(LocalDate date) {
        List<DateRecord> records = recordService.findDateRecords(date);
        log.debug("[计算碟片][共{}个]", records.size());
        records.forEach(record0 -> {
            Disc disc = record0.getDisc();
            DateRecord record1 = recordService.getDateRecord(disc, date.minusDays(1));
            DateRecord record7 = recordService.getDateRecord(disc, date.minusDays(7));
            computePt(disc, date, record0, record1, record7);
        });
    }

    @Transactional
    public void computePtNow(Disc disc, LocalDate date, int hour) {
        HourRecord record0 = recordService.getOrCreateHourRecord(disc, date);
        DateRecord record1 = recordService.getDateRecord(disc, date.minusDays(1));
        DateRecord record7 = recordService.getDateRecord(disc, date.minusDays(7));

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
            record.setTodayPt(24 * computeHourPt(record.getDisc(), rank));
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
            double addPt = (record0.getTotalPt() - record7.getTotalPt()) / 7d;
            LocalDate releaseDate = record0.getDisc().getReleaseDate();
            LocalDate currentDate = record0.getDate();
            long days = releaseDate.toEpochDay() - currentDate.toEpochDay() - 1;
            record0.setGuessPt(record0.getTotalPt() + addPt * days);
        }
    }

    private static double computeHourPt(Disc disc, double rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computeHourPt(150, 5.25, rank);
            case Auto:
            case Bluray:
                return computePtOfBD(rank);
            case Dvd:
                return computeHourPt(100, 4.2, rank);
            default:
                return 0d;
        }
    }

    private static double computePtOfBD(double rank) {
        if (rank <= 10) {
            return computeHourPt(100, 3.2, rank);
        } else if (rank <= 20) {
            return computeHourPt(100, 3.3, rank);
        } else if (rank <= 50) {
            return computeHourPt(100, 3.4, rank);
        } else if (rank <= 100) {
            return computeHourPt(100, 3.6, rank);
        } else if (rank <= 300) {
            return computeHourPt(100, 3.8, rank);
        } else {
            return computeHourPt(100, 3.9, rank);
        }
    }

    private static double computeHourPt(int div, double base, double rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

    private static Integer safeIntValue(Double value) {
        return Optional.ofNullable(value).map(Double::intValue).orElse(null);
    }

}
