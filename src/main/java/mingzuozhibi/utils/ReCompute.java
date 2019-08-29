package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.rank.DateRecord;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static mingzuozhibi.utils.RecordUtils.findDateRecord;

@Service
public class ReCompute {

    public static final Logger LOGGER = LoggerFactory.getLogger(ReCompute.class);

    @Autowired
    private Dao dao;

    public void reComputeDateRecords(LocalDate date) {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<DateRecord> dateRecords = session.createCriteria(DateRecord.class)
                    .add(Restrictions.eq("date", date))
                    .list();

            dateRecords.forEach(dateRecord0 -> {
                Disc disc = dateRecord0.getDisc();
                if (date.isBefore(disc.getReleaseDate())) {
                    computeTodayPt(dateRecord0);
                    computeTotalPt(dateRecord0, findDateRecord(dao, disc, date.minusDays(1)));
                    computeGuessPt(dateRecord0, findDateRecord(dao, disc, date.minusDays(7)));
                } else {
                    DateRecord dateRecord1 = findDateRecord(dao, disc, date.minusDays(1));
                    dateRecord0.setTodayPt(null);
                    dateRecord0.setTotalPt(dateRecord1.getTotalPt());
                    dateRecord0.setGuessPt(dateRecord1.getGuessPt());
                }
            });
            LOGGER.info("[手动任务][重新计算{}的数据][共{}个]", date, dateRecords.size());
        });
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
        }
    }

    private void computeTodayPt(DateRecord dateRecord) {
        Optional.ofNullable(dateRecord.getRank()).ifPresent(rank -> {
            dateRecord.setTodayPt(24 * computeHourPt(dateRecord.getDisc(), rank.intValue()));
        });
    }

    private double computeHourPt(Disc disc, int rank) {
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

    private double computePtOfBD(int rank) {
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

    private double computeHourPt(int div, double base, int rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

}
