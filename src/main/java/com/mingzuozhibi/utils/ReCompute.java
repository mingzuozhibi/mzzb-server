package com.mingzuozhibi.utils;

import com.mingzuozhibi.modules.core.disc.Disc;
import com.mingzuozhibi.modules.core.record.DateRecord;
import com.mingzuozhibi.support.Dao;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.mingzuozhibi.utils.RecordUtils.findDateRecord;

@Service
public class ReCompute {

    public static final Logger LOGGER = LoggerFactory.getLogger(ReCompute.class);

    @Autowired
    private Dao dao;

    public void reComputeDateRecords(Disc disc) {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<DateRecord> dateRecords = session.createCriteria(DateRecord.class)
                .add(Restrictions.eq("disc", disc))
                .addOrder(Order.asc("date"))
                .list();

            dateRecords.forEach(dateRecord -> {
                reCompute(disc, dateRecord.getDate(), dateRecord);
            });

            DateRecord dateRecord = dateRecords.get(dateRecords.size() - 1);

            disc.setTodayPt(safeIntValue(dateRecord.getTodayPt()));
            disc.setTotalPt(safeIntValue(dateRecord.getTotalPt()));
            disc.setGuessPt(safeIntValue(dateRecord.getGuessPt()));
        });
    }

    public static Integer safeIntValue(Double value) {
        return Optional.ofNullable(value).map(Double::intValue).orElse(null);
    }

    public void reComputeDateRecords(LocalDate date) {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<DateRecord> dateRecords = session.createCriteria(DateRecord.class)
                .add(Restrictions.eq("date", date))
                .list();

            dateRecords.forEach(dateRecord -> {
                reCompute(dateRecord.getDisc(), date, dateRecord);
            });
            LOGGER.info("[手动任务][重新计算{}的数据][共{}个]", date, dateRecords.size());
        });
    }

    private void reCompute(Disc disc, LocalDate date, DateRecord dateRecord) {
        if (date.isBefore(disc.getReleaseDate())) {
            computeTodayPt(dateRecord);
            computeTotalPt(dateRecord, findDateRecord(dao, disc, date.minusDays(1)));
            computeGuessPt(dateRecord, findDateRecord(dao, disc, date.minusDays(7)));
        } else {
            DateRecord dateRecord1 = findDateRecord(dao, disc, date.minusDays(1));
            dateRecord.setTodayPt(null);
            dateRecord.setTotalPt(dateRecord1.getTotalPt());
            dateRecord.setGuessPt(dateRecord1.getGuessPt());
        }
    }

    private void computeGuessPt(DateRecord dateRecord0, DateRecord dateRecord7) {
        if (dateRecord7 == null) {
            return;
        }
        if (dateRecord0.getTotalPt() != null && dateRecord7.getTotalPt() != null) {
            double addPt = (dateRecord0.getTotalPt() - dateRecord7.getTotalPt()) / 7d;
            LocalDate releaseDate = dateRecord0.getDisc().getReleaseDate();
            LocalDate currentDate = dateRecord0.getDate();
            long days = getDays(releaseDate, currentDate);
            dateRecord0.setGuessPt(dateRecord0.getTotalPt() + addPt * days);
        }
    }

    private long getDays(LocalDate releaseDate, LocalDate currentDate) {
        return releaseDate.toEpochDay() - currentDate.toEpochDay() - 1;
    }

    private void computeTotalPt(DateRecord dateRecord0, DateRecord dateRecord1) {
        if (dateRecord1 == null || dateRecord1.getTotalPt() == null) {
            dateRecord0.setTotalPt(dateRecord0.getTodayPt());
        } else if (dateRecord0.getTodayPt() != null) {
            dateRecord0.setTotalPt(dateRecord0.getTodayPt() + dateRecord1.getTotalPt());
        } else {
            dateRecord0.setTotalPt(dateRecord1.getTotalPt());
        }
    }

    private void computeTodayPt(DateRecord dateRecord) {
        Optional.ofNullable(dateRecord.getRank()).ifPresent(rank -> {
            dateRecord.setTodayPt(24 * computeHourPt(dateRecord.getDisc(), rank));
        });
    }

    public static double computeHourPt(Disc disc, double rank) {
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

}
