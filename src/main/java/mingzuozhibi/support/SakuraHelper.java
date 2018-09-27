package mingzuozhibi.support;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public abstract class SakuraHelper {

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

    public static JSONArray buildRecords(Dao dao, Disc disc) {
        JSONArray array = new JSONArray();
        @SuppressWarnings("unchecked")
        List<Record> records = dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .addOrder(Order.desc("date"))
                .list();
        records.forEach(record -> {
            JSONObject object = new JSONObject();
            object.put("id", record.getId());
            object.put("date", record.getDate());
            object.put("todayPt", record.getTodayPt());
            object.put("totalPt", record.getTotalPt());
            getAverRank(record).ifPresent(averRank -> {
                object.put("averRank", (int) averRank);
            });
            array.put(object);
        });
        return array;
    }

    private static OptionalDouble getAverRank(Record record) {
        IntStream.Builder builder = IntStream.builder();
        for (int i = 0; i < 24; i++) {
            Integer rank = record.getRank(i);
            if (rank != null && rank != 0) {
                builder.add(rank);
            }
        }
        return builder.build().average();
    }

    public static JSONArray buildRanks(Dao dao, Disc disc) {
        JSONArray array = new JSONArray();
        findRanks(dao, disc).forEach(record -> {
            for (int i = 0; i < 24; i++) {
                if (array.length() >= 5) {
                    break;
                }
                int hour = 23 - i;
                Integer rank = record.getRank(hour);
                if (rank != null && rank != 0) {
                    JSONObject object = new JSONObject();
                    object.put("date", record.getDate());
                    object.put("hour", String.format("%02d", hour));
                    object.put("rank", rank);
                    array.put(object);
                }
            }
        });
        return array;
    }

    @SuppressWarnings("unchecked")
    public static List<Record> findRanks(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.desc("date"))
                .setMaxResults(2)
                .list();
    }

    public static void computeAndUpdateAmazonPt(Dao dao, Disc disc) {
        AtomicReference<Integer> lastRank = new AtomicReference<>();
        AtomicReference<Double> lastTotalPt = new AtomicReference<>(0d);

        LocalDate today = LocalDate.now();
        LocalDate seven = today.minusDays(7);
        AtomicReference<Integer> sevenPt = new AtomicReference<>();

        disc.setTodayPt(null);

        findRecords(dao, disc).forEach(record -> {
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

    @SuppressWarnings("unchecked")
    private static List<Record> findRecords(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.asc("date"))
                .list();
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

    public static boolean noExpiredDisc(Disc disc) {
        return disc.getReleaseDate().isAfter(LocalDate.now().minusDays(7));
    }

}
