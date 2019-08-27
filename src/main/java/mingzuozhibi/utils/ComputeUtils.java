package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.rank.DateRecord;
import mingzuozhibi.persist.rank.HourRecord;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Restrictions;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ComputeUtils {

    public static void computeAndUpdateAmazonPt(Dao dao, Disc disc) {
        LocalDate today = LocalDate.now();
        dao.execute(session -> {
            HourRecord hourRecord = (HourRecord) session.createCriteria(HourRecord.class)
                    .add(Restrictions.eq("disc", disc))
                    .add(Restrictions.eq("date", today))
                    .uniqueResult();
            if (hourRecord == null) {
                return;
            }

            AtomicReference<Double> todayPtRef = new AtomicReference<>();
            AtomicReference<Double> totalPtRef = new AtomicReference<>();

            DateRecord dateRecord_1 = (DateRecord) session.createCriteria(DateRecord.class)
                    .add(Restrictions.eq("disc", disc))
                    .add(Restrictions.eq("date", today.minusDays(1)))
                    .uniqueResult();
            if (dateRecord_1 != null) {
                totalPtRef.set(dateRecord_1.getTotalPt());
            }

            hourRecord.getAverRank().ifPresent(rank -> {
                if (hourRecord.getDate().isBefore(disc.getReleaseDate())) {
                    todayPtRef.set(computeHourPt(disc, (int) rank) * 24);
                    hourRecord.setTodayPt(todayPtRef.get());
                    disc.setTodayPt(todayPtRef.get().intValue());

                    if (totalPtRef.get() != null) {
                        totalPtRef.set(totalPtRef.get() + todayPtRef.get());
                    } else {
                        totalPtRef.set(todayPtRef.get());
                    }
                } else {
                    disc.setTodayPt(null);
                    hourRecord.setTodayPt(null);
                }
                hourRecord.setTotalPt(totalPtRef.get());
                disc.setTotalPt(totalPtRef.get().intValue());

                DateRecord dateRecord_7 = (DateRecord) session.createCriteria(DateRecord.class)
                        .add(Restrictions.eq("disc", disc))
                        .add(Restrictions.eq("date", today.minusDays(7)))
                        .uniqueResult();
                if (dateRecord_7 != null) {
                    Double sevenPt = dateRecord_7.getTotalPt();
                    if (sevenPt != null) {
                        updateGuessPt(disc, today, totalPtRef.get(), sevenPt);
                    }
                }
            });
        });
    }

    private static void updateGuessPt(Disc disc, LocalDate today, Double totalPt, Double sevenPt) {
        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay() - 1;
        if (days <= 0) {
            disc.setGuessPt(totalPt.intValue());
        } else if (sevenPt != null) {
            disc.setGuessPt((int) (totalPt + (totalPt - sevenPt) / 7d * days));
        }
    }

    private static double computeHourPt(Disc disc, int rank) {
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

    private static double computePtOfBD(int rank) {
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

    private static double computeHourPt(int div, double base, int rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

}
