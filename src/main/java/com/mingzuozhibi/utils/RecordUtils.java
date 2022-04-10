package com.mingzuozhibi.utils;

import com.mingzuozhibi.modules.core.disc.Disc;
import com.mingzuozhibi.modules.core.record.DateRecord;
import com.mingzuozhibi.modules.core.record.HourRecord;
import com.mingzuozhibi.support.Dao;
import org.hibernate.criterion.Restrictions;

import java.time.LocalDate;

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

}
