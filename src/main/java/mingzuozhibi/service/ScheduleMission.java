package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.rank.DateRecord;
import mingzuozhibi.persist.rank.HourRecord;
import mingzuozhibi.persist.user.AutoLogin;
import mingzuozhibi.support.Dao;
import mingzuozhibi.utils.DiscUtils;
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

import static mingzuozhibi.utils.DiscGroupUtils.computeAndUpdateAmazonPt;
import static mingzuozhibi.utils.RecordUtils.getOrCreateRecord;

@Service
public class ScheduleMission {

    public static final Logger LOGGER = LoggerFactory.getLogger(ScheduleMission.class);

    @Autowired
    private Dao dao;

    public void removeExpiredAutoLoginData() {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<AutoLogin> expired = session.createCriteria(AutoLogin.class)
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
                hourRecord.getAverRank().ifPresent(rank -> {
                    dateRecord.setRank((int) rank);
                });
                Optional.ofNullable(hourRecord.getTodayPt()).ifPresent(todayPt -> {
                    dateRecord.setTodayPt(todayPt.doubleValue());
                });
                Optional.ofNullable(hourRecord.getTotalPt()).ifPresent(totalPt -> {
                    dateRecord.setTotalPt(totalPt.doubleValue());
                });
                session.delete(hourRecord);
                session.save(dateRecord);
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
            Set<Disc> discs = DiscUtils.needRecordDiscs(session);

            discs.forEach(disc -> {
                HourRecord hourRecord = getOrCreateRecord(dao, disc, date);
                hourRecord.setRank(hour, disc.getThisRank());
                hourRecord.setTotalPt(disc.getTotalPt());
            });
            LOGGER.info("[定时任务][记录碟片排名][共{}个]", discs.size());

            discs.forEach(disc -> {
                computeAndUpdateAmazonPt(dao, disc);
            });
            LOGGER.info("[定时任务][计算任务完成][共{}个]", discs.size());
        });
    }

}
