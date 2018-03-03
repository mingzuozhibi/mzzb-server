package mingzuozhibi.service;

import mingzuozhibi.persist.AutoLogin;
import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Sakura;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static mingzuozhibi.persist.Sakura.ViewType.SakuraList;

@Service
public class EveryHourTask {

    @Autowired
    private Dao dao;
    public static final Logger LOGGER = LoggerFactory.getLogger(EveryHourTask.class);

    @Transactional
    public void run() {
        doEveryHourTask();
    }

    private void doEveryHourTask() {
        dao.execute(session -> {
            removeReleasedTenDaysDiscsFromSakura();
            removeExpiredAutoLoginData();
        });
    }

    private void removeExpiredAutoLoginData() {
        @SuppressWarnings("unchecked")
        List<AutoLogin> expired = dao.query(session -> {
            return session.createCriteria(AutoLogin.class)
                    .add(Restrictions.lt("expired", LocalDateTime.now()))
                    .list();
        });
        expired.forEach(autoLogin -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[定时任务][移除过期的AutoLogin数据][id={}, username={}]",
                        autoLogin.getId(), autoLogin.getUser().getUsername());
            }
            dao.delete(autoLogin);
        });
    }

    private void removeReleasedTenDaysDiscsFromSakura() {
        @SuppressWarnings("unchecked")
        List<Sakura> sakuras = dao.query(session -> {
            return session.createCriteria(Sakura.class)
                    .add(Restrictions.ne("key", "9999-99"))
                    .add(Restrictions.eq("enabled", true))
                    .add(Restrictions.eq("viewType", SakuraList))
                    .list();
        });

        sakuras.forEach(sakura -> {
            List<Disc> toDelete = sakura.getDiscs().stream()
                    .filter(this::isReleasedTenDays)
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
    }

    private boolean isReleasedTenDays(Disc disc) {
        LocalDate releaseTenDays = disc.getReleaseDate().plusDays(10);
        return LocalDate.now().isAfter(releaseTenDays);
    }

}
