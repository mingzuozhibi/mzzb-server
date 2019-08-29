package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.rank.DateRecord;
import mingzuozhibi.persist.rank.HourRecord;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public abstract class RecordUtils {

    public static HourRecord findOrCreateHourRecord(Dao dao, Disc disc, LocalDate date) {
        HourRecord hourRecord = (HourRecord) dao.create(HourRecord.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.eq("date", date))
                .uniqueResult();
        if (hourRecord == null) {
            hourRecord = new HourRecord(disc, date);
            dao.save(hourRecord);
        }
        return hourRecord;
    }

    public static DateRecord findDateRecord(Dao dao, Disc disc, LocalDate date) {
        return (DateRecord) dao.create(DateRecord.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.eq("date", date))
                .uniqueResult();
    }

    public static JSONArray buildRecords(Dao dao, Disc disc) {
        JSONArray array = new JSONArray();

        HourRecord hourRecord = (HourRecord) dao.create(HourRecord.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.eq("date", LocalDate.now()))
                .uniqueResult();

        if (hourRecord != null) {
            JSONObject object = new JSONObject();
            object.put("id", hourRecord.getId());
            object.put("date", hourRecord.getDate());
            hourRecord.getAverRank().ifPresent(rank -> {
                object.put("averRank", (int) rank);
            });
            Optional.ofNullable(hourRecord.getTodayPt()).ifPresent(addPt -> {
                object.put("todayPt", addPt.intValue());
            });
            Optional.ofNullable(hourRecord.getTotalPt()).ifPresent(sumPt -> {
                object.put("totalPt", sumPt.intValue());
            });
            Optional.ofNullable(hourRecord.getGuessPt()).ifPresent(gesPt -> {
                object.put("guessPt", gesPt.intValue());
            });
            array.put(object);
        }

        @SuppressWarnings("unchecked")
        List<DateRecord> dateRecords = dao.create(DateRecord.class)
                .add(Restrictions.eq("disc", disc))
                .addOrder(Order.desc("date"))
                .list();

        dateRecords.forEach(dateRecord -> {
            JSONObject object = new JSONObject();
            object.put("id", dateRecord.getId());
            object.put("date", dateRecord.getDate());
            Optional.ofNullable(dateRecord.getRank()).ifPresent(rank -> {
                object.put("averRank", rank.intValue());
            });
            Optional.ofNullable(dateRecord.getTodayPt()).ifPresent(addPt -> {
                object.put("todayPt", addPt.intValue());
            });
            Optional.ofNullable(dateRecord.getTotalPt()).ifPresent(sumPt -> {
                object.put("totalPt", sumPt.intValue());
            });
            Optional.ofNullable(dateRecord.getGuessPt()).ifPresent(gesPt -> {
                object.put("guessPt", gesPt.intValue());
            });
            array.put(object);
        });

        return array;
    }

}
