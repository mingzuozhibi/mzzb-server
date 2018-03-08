package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SakuraHelper {

    public static boolean isExpiredSakura(Sakura sakura) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate sakuraDate = LocalDate.parse(sakura.getKey() + "-01", formatter);
        return LocalDate.now().isAfter(sakuraDate.plusMonths(3));
    }

    public static boolean isExpiredDisc(Disc disc) {
        return disc.getReleaseDate().isBefore(LocalDate.now().minusDays(7));
    }

    public static boolean noExpiredDisc(Disc disc) {
        return !isExpiredDisc(disc);
    }

    public static Record getOrCreateRecord(Dao dao, Disc disc, LocalDate date) {
        Record record = (Record) dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.eq("date", date))
                .uniqueResult();
        if (record == null) {
            record = new Record(disc, date);
            dao.save(record);
        }
        return record;
    }

    @SuppressWarnings("unchecked")
    public static List<Record> getRecords(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.asc("date"))
                .list();
    }

    public static void computeAndUpdateSakuraPt(Disc disc, List<Record> records) {
        AtomicReference<Integer> lastTotalPt = new AtomicReference<>();

        LocalDateTime japanTime = LocalDateTime.now().plusHours(1);
        LocalDate today = japanTime.toLocalDate();
        LocalDate seven = japanTime.minusDays(7).toLocalDate();
        AtomicReference<Integer> sevenPt = new AtomicReference<>();

        records.forEach(record -> {
            LocalDate recordDate = record.getDate();
            if (record.getTotalPt() != null) {
                if (lastTotalPt.get() != null) {
                    record.setTodayPt(record.getTotalPt() - lastTotalPt.get());
                    if (recordDate.equals(today)) {
                        disc.setTodayPt(record.getTodayPt());
                    }
                    if (recordDate.equals(seven)) {
                        sevenPt.set(record.getTotalPt());
                    }
                }
                lastTotalPt.set(record.getTotalPt());
            }
        });

        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay();
        if (days <= 0) {
            disc.setGuessPt(lastTotalPt.get());
        } else if (sevenPt.get() != null) {
            disc.setGuessPt((int) (lastTotalPt.get() + (lastTotalPt.get() - sevenPt.get()) / 7d * days));
        }
    }

    public static void computeAndUpdateAmazonPt(Disc disc, List<Record> records) {
        AtomicReference<Integer> lastRank = new AtomicReference<>();
        AtomicReference<Double> totalPt = new AtomicReference<>(0d);

        LocalDateTime japanTime = LocalDateTime.now().plusHours(1);
        LocalDate today = japanTime.toLocalDate();
        LocalDate seven = japanTime.minusDays(7).toLocalDate();
        AtomicReference<Double> sevenPt = new AtomicReference<>();

        disc.setTodayPt(null);

        records.forEach(record -> {
            double todayPt = computeRecordPt(disc, record, lastRank);
            totalPt.set(totalPt.get() + todayPt);
            record.setTotalPt(totalPt.get().intValue());
            record.setTodayPt((int) todayPt);
            if (record.getDate().equals(today)) {
                disc.setTodayPt((int) todayPt);
            }
            if (record.getDate().equals(seven)) {
                sevenPt.set(totalPt.get());
            }
        });

        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay();
        if (days <= 0) {
            disc.setGuessPt(totalPt.get().intValue());
        } else if (sevenPt.get() != null) {
            disc.setGuessPt((int) (totalPt.get() + (totalPt.get() - sevenPt.get()) / 7 * days));
        }
        disc.setTotalPt(totalPt.get().intValue());
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
            if (rank != null) {
                recordPt += computeHourPt(disc, rank);
            }
        }
        return recordPt;
    }

    private static double computeHourPt(Disc disc, Integer rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computePt(150, 5.25, rank);
            case Other:
                return 0d;
            default:
                if (disc.getTitle().contains("Blu-ray")) {
                    if (rank <= 10) {
                        return computePt(100, 3.2, rank);
                    } else if (rank <= 20) {
                        return computePt(100, 3.3, rank);
                    } else if (rank <= 50) {
                        return computePt(100, 3.4, rank);
                    } else if (rank <= 100) {
                        return computePt(100, 3.6, rank);
                    } else if (rank <= 300) {
                        return computePt(100, 3.8, rank);
                    } else {
                        return computePt(100, 3.9, rank);
                    }
                } else {
                    return computePt(100, 4.2, rank);
                }
        }
    }

    private static double computePt(int div, double base, int rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

}
