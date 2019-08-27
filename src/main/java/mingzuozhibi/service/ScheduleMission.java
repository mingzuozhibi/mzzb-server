package mingzuozhibi.service;

import mingzuozhibi.persist.user.AutoLogin;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.support.Dao;
import mingzuozhibi.utils.DiscUtils;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

            expired.forEach(autoLogin -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[定时任务][移除过期的AutoLogin数据][id={}, username={}]",
                            autoLogin.getId(), autoLogin.getUser().getUsername());
                }
                dao.delete(autoLogin);
            });
        });
    }

    public void recordDiscsRankAndComputePt() {
        new Thread(() -> {
            // +9 timezone and prev hour, so +1h -1h = +0h
            LocalDateTime recordTime = LocalDateTime.now();
            LocalDate date = recordTime.toLocalDate();
            int hour = recordTime.getHour();

            dao.execute(session -> {
                Set<Disc> discs = DiscUtils.needRecordDiscs(session);

                LOGGER.info("[定时任务][记录碟片排名][共{}个]", discs.size());

                discs.forEach(disc -> {
                    Record record = getOrCreateRecord(dao, disc, date);
                    record.setRank(hour, disc.getThisRank());
                    record.setTotalPt(disc.getTotalPt());

                    session.flush();
                    session.evict(record);
                });

                LOGGER.info("[定时任务][计算碟片PT][碟片数量为:{}]", discs.size());

                discs.forEach(disc -> {
                    computeAndUpdateAmazonPt(dao, disc);
                });
                LOGGER.info("[定时任务][计算碟片PT完成][共{}个]", discs.size());
            });
        }).start();
    }

}
