package com.mingzuozhibi.service;

import com.mingzuozhibi.support.Dao;
import com.mingzuozhibi.persist.disc.Disc;
import com.mingzuozhibi.persist.rank.DateRecord;
import com.mingzuozhibi.persist.rank.HourRecord;
import com.mingzuozhibi.modules.user.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.mingzuozhibi.utils.DiscUtils.needRecordDiscs;
import static com.mingzuozhibi.utils.ReCompute.computeHourPt;
import static com.mingzuozhibi.utils.ReCompute.safeIntValue;
import static com.mingzuozhibi.utils.RecordUtils.findDateRecord;
import static com.mingzuozhibi.utils.RecordUtils.findOrCreateHourRecord;

@Service
public class ScheduleMission {

    public static final Logger LOGGER = LoggerFactory.getLogger(ScheduleMission.class);

    @Autowired
    private Dao dao;

    public void removeExpiredAutoLoginData() {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<Session> expired = session.createCriteria(Session.class)
                .add(Restrictions.lt("expired", LocalDateTime.now()))
                .list();
            expired.forEach(autoLogin -> dao.delete(autoLogin));
            LOGGER.info("[定时任务][清理自动登入][共{}个]", expired.size());
        });
    }

    public void moveHourRecordToDateRecord() {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<HourRecord> hourRecords = session.createCriteria(HourRecord.class)
                .add(Restrictions.lt("date", LocalDate.now()))
                .addOrder(Order.asc("date"))
                .list();
            hourRecords.forEach(hourRecord -> {
                DateRecord dateRecord = new DateRecord(hourRecord.getDisc(), hourRecord.getDate());
                hourRecord.getAverRank().ifPresent(dateRecord::setRank);
                Optional.ofNullable(hourRecord.getTodayPt()).ifPresent(dateRecord::setTodayPt);
                Optional.ofNullable(hourRecord.getTotalPt()).ifPresent(dateRecord::setTotalPt);
                Optional.ofNullable(hourRecord.getGuessPt()).ifPresent(dateRecord::setGuessPt);
                session.save(dateRecord);
                session.delete(hourRecord);
            });
            LOGGER.info("[定时任务][转录碟片排名][共{}个]", hourRecords.size());
        });
    }

    public void recordDiscsRankAndComputePt() {
        // +9 timezone and prev hour, so +1h -1h = +0h
        LocalDateTime recordTime = LocalDateTime.now();
        LocalDate date = recordTime.toLocalDate();
        int hour = recordTime.getHour();

        dao.execute(session -> {
            Set<Disc> discs = needRecordDiscs(session);

            discs.forEach(disc -> {
                HourRecord hourRecord = findOrCreateHourRecord(dao, disc, date);
                hourRecord.setRank(hour, disc.getThisRank());
                if (date.isBefore(disc.getReleaseDate())) {
                    computeTodayPt(hourRecord);
                    computeTotalPt(hourRecord, findDateRecord(dao, disc, date.minusDays(1)));
                    computeGuessPt(hourRecord, findDateRecord(dao, disc, date.minusDays(7)));

                    disc.setTodayPt(safeIntValue(hourRecord.getTodayPt()));
                    disc.setTotalPt(safeIntValue(hourRecord.getTotalPt()));
                    disc.setGuessPt(safeIntValue(hourRecord.getGuessPt()));
                } else {
                    DateRecord dateRecord = findDateRecord(dao, disc, date.minusDays(1));
                    if (dateRecord == null) {
                        return;
                    }

                    hourRecord.setTodayPt(null);
                    hourRecord.setTotalPt(dateRecord.getTotalPt());
                    hourRecord.setGuessPt(dateRecord.getGuessPt());

                    disc.setTodayPt(null);
                    disc.setTotalPt(safeIntValue(dateRecord.getTotalPt()));
                    disc.setGuessPt(safeIntValue(dateRecord.getGuessPt()));
                }

            });
            LOGGER.info("[定时任务][记录碟片排名][共{}个]", discs.size());
        });
    }


    private void computeGuessPt(HourRecord hourRecord0, DateRecord dateRecord7) {
        if (dateRecord7 == null) {
            return;
        }
        if (hourRecord0.getTotalPt() != null && dateRecord7.getTotalPt() != null) {
            double addPt = (hourRecord0.getTotalPt() - dateRecord7.getTotalPt()) / 7d;
            LocalDate releaseDate = hourRecord0.getDisc().getReleaseDate();
            LocalDate currentDate = hourRecord0.getDate();
            long days = getDays(releaseDate, currentDate);
            hourRecord0.setGuessPt(hourRecord0.getTotalPt() + addPt * days);
        }
    }

    private long getDays(LocalDate releaseDate, LocalDate currentDate) {
        return releaseDate.toEpochDay() - currentDate.toEpochDay() - 1;
    }

    private void computeTotalPt(HourRecord hourRecord, DateRecord dateRecord) {
        if (dateRecord == null || dateRecord.getTotalPt() == null) {
            hourRecord.setTotalPt(hourRecord.getTodayPt());
        } else if (hourRecord.getTodayPt() != null) {
            hourRecord.setTotalPt(hourRecord.getTodayPt() + dateRecord.getTotalPt());
        } else {
            hourRecord.setTotalPt(dateRecord.getTotalPt());
        }
    }

    private void computeTodayPt(HourRecord hourRecord) {
        hourRecord.getAverRank().ifPresent(rank -> {
            hourRecord.setTodayPt(24 * computeHourPt(hourRecord.getDisc(), rank));
        });
    }

}
