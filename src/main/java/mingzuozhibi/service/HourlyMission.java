package mingzuozhibi.service;

import mingzuozhibi.persist.core.AutoLogin;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static mingzuozhibi.persist.disc.Sakura.ViewType.SakuraList;

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
                    .add(Restrictions.eq("viewType", SakuraList))
                    .list();

            sakuras.forEach(sakura -> {
                List<Disc> toDelete = sakura.getDiscs().stream()
                        .filter(this::notSakuraUpdateType)
                        .filter(this::isReleasedSevenDays)
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

    private boolean isReleasedSevenDays(Disc disc) {
        LocalDate releaseTenDays = disc.getReleaseDate().plusDays(7);
        return LocalDate.now().isAfter(releaseTenDays);
    }

    private boolean notSakuraUpdateType(Disc disc) {
        return disc.getUpdateType() != Disc.UpdateType.Sakura;
    }

}
