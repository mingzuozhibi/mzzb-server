package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

public abstract class RecordUtils {

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
    private static List<Record> findRanks(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.desc("date"))
                .setMaxResults(2)
                .list();
    }

    @SuppressWarnings("unchecked")
    public static List<Record> findRecords(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.asc("date"))
                .list();
    }
}
