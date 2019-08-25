package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.support.Dao;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SakuraUtils {

    public static void computeAndUpdateAmazonPt(Dao dao, Disc disc) {
        AtomicReference<Integer> lastRank = new AtomicReference<>();
        AtomicReference<Double> lastTotalPt = new AtomicReference<>(0d);

        LocalDate today = LocalDate.now();
        LocalDate seven = today.minusDays(7);
        AtomicReference<Integer> sevenPt = new AtomicReference<>();

        disc.setTodayPt(null);

        RecordUtils.findRecords(dao, disc).forEach(record -> {
            double todayPt = computeRecordPt(disc, record, lastRank);
            double totalPt = lastTotalPt.get() + todayPt;
            lastTotalPt.set(totalPt);
            record.setTotalPt((int) totalPt);
            record.setTodayPt((int) todayPt);
            checkToday(record, today, disc);
            checkSeven(record, seven, sevenPt);

            dao.session().flush();
            dao.session().evict(record);
        });

        disc.setTotalPt(lastTotalPt.get().intValue());

        updateGuessPt(disc, today, lastTotalPt.get().intValue(), sevenPt.get());
    }

    private static void checkToday(Record record, LocalDate today, Disc disc) {
        if (record.getDate().equals(today)) {
            disc.setTodayPt(record.getTodayPt());
        }
    }

    private static void checkSeven(Record record, LocalDate seven, AtomicReference<Integer> sevenPt) {
        if (record.getDate().equals(seven)) {
            sevenPt.set(record.getTotalPt());
        }
    }

    private static void updateGuessPt(Disc disc, LocalDate today, Integer totalPt, Integer sevenPt) {
        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay() - 1;
        if (days <= 0) {
            disc.setGuessPt(totalPt);
        } else if (sevenPt != null) {
            disc.setGuessPt((int) (totalPt + (totalPt - sevenPt) / 7d * days));
        }
    }

    private static double computeRecordPt(Disc disc, Record record, AtomicReference<Integer> lastRank) {
        double recordPt = 0d;
        for (int i = 0; i < 24; i++) {
            Integer rank = record.getRank(i);
            if (rank == null) {
                rank = lastRank.get();
            } else {
                lastRank.set(rank);
            }
            if (rank != null && rank != 0) {
                recordPt += computeHourPt(disc, rank);
            }
        }
        return recordPt;
    }

    private static double computeHourPt(Disc disc, int rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computeHourPt(150, 5.25, rank);
            case Auto:
                return computePtOfBD(rank);
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

    @SuppressWarnings("unchecked")
    public static List<Sakura> sakurasOrderByDescKey(Session session) {
        return session.createCriteria(Sakura.class)
                .add(Restrictions.eq("enabled", true))
                .addOrder(Order.desc("key"))
                .list();
    }

}
