package mingzuozhibi.service;

import mingzuozhibi.persist.core.AutoLogin;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.persist.disc.Sakura.ViewType;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.SakuraHelper;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static mingzuozhibi.support.SakuraHelper.*;

@Service
public class HourlyMission {

    @Autowired
    private Dao dao;
    public static final Logger LOGGER = LoggerFactory.getLogger(HourlyMission.class);

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

    public void removeExpiredDiscsFromList() {
        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<Sakura> sakuras = session.createCriteria(Sakura.class)
                    .add(Restrictions.ne("key", "9999-99"))
                    .add(Restrictions.eq("enabled", true))
                    .add(Restrictions.eq("viewType", ViewType.SakuraList))
                    .list();

            sakuras.forEach(sakura -> {
                boolean expiredSakura = isExpiredSakura(sakura);
                List<Disc> toDelete = sakura.getDiscs().stream()
                        .filter(disc -> disc.getUpdateType() != UpdateType.Sakura)
                        .filter(disc -> disc.getUpdateType() == UpdateType.None || expiredSakura)
                        .filter(SakuraHelper::isExpiredDisc)
                        .collect(Collectors.toList());
                sakura.getDiscs().removeAll(toDelete);

                if (sakura.getDiscs().isEmpty()) {
                    sakura.setEnabled(false);
                }

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("[定时任务][移除过期的Sakura碟片][sakura={}, delete={}]",
                            sakura.getTitle(), toDelete.size());
                    toDelete.forEach(disc -> LOGGER.info("[移除碟片][sakura={}, disc={}]",
                            sakura.getTitle(), disc.getTitle()));
                    if (!sakura.isEnabled() && toDelete.size() > 0) {
                        LOGGER.info("[Sakura列表为空: setEnabled(false)]");
                    }
                }
            });
        });
    }

    public void recordDiscsRankAndComputePt() {
        // +9 timezone and prev hour, so +1h -1h = +0h
        LocalDateTime recordTime = LocalDateTime.now();
        LocalDate date = recordTime.toLocalDate();
        int hour = recordTime.getHour();

        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<Sakura> sakuras = session.createCriteria(Sakura.class)
                    .add(Restrictions.ne("key", "9999-99"))
                    .add(Restrictions.eq("enabled", true))
                    .list();

            Set<Disc> discs = new LinkedHashSet<>();

            sakuras.forEach(sakura -> {
                sakura.getDiscs().stream()
                        .filter(disc -> disc.getUpdateType() != UpdateType.None)
                        .filter(disc -> disc.getReleaseDate().isAfter(date))
                        .forEach(discs::add);
            });

            LOGGER.info("[定时任务][记录碟片排名][碟片数量为:{}]", discs.size());

            discs.forEach(disc -> {
                Record record = getOrCreateRecord(dao, disc, date);
                record.setRank(hour, disc.getThisRank());
                if (disc.getUpdateType() == UpdateType.Sakura) {
                    record.setTotalPt(disc.getTotalPt());
                }
            });

            LOGGER.info("[定时任务][计算碟片PT][碟片数量为:{}]", discs.size());

            discs.forEach(disc -> {
                if (disc.getUpdateType() != UpdateType.Sakura) {
                    computeAndUpdateAmazonPt(dao, disc);
                } else {
                    computeAndUpdateSakuraPt(dao, disc);
                }
            });
        });
    }


}
