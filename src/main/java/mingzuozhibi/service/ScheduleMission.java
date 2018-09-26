package mingzuozhibi.service;

import mingzuozhibi.persist.core.AutoLogin;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.persist.disc.Sakura.ViewType;
import mingzuozhibi.service.amazon.AmazonTaskService;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.SakuraHelper;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static mingzuozhibi.action.DiscController.*;
import static mingzuozhibi.service.amazon.DocumentReader.getNode;
import static mingzuozhibi.service.amazon.DocumentReader.getText;
import static mingzuozhibi.support.SakuraHelper.*;

@Service
public class ScheduleMission {

    public static final Logger LOGGER = LoggerFactory.getLogger(ScheduleMission.class);

    @Autowired
    private Dao dao;

    @Autowired
    private AmazonTaskService service;

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

                session.flush();
                session.clear();
                Thread.yield();
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
                        .filter(SakuraHelper::noExpiredDisc)
                        .forEach(discs::add);
            });

            LOGGER.info("[定时任务][记录碟片排名][碟片数量为:{}]", discs.size());

            discs.forEach(disc -> {
                Record record = getOrCreateRecord(dao, disc, date);
                record.setRank(hour, disc.getThisRank());
                record.setTotalPt(disc.getTotalPt());

                session.flush();
                session.clear();
                Thread.yield();
            });

            LOGGER.info("[定时任务][计算碟片PT][碟片数量为:{}]", discs.size());

            discs.forEach(disc -> {
                if (disc.getUpdateType() != UpdateType.Sakura) {
                    computeAndUpdateAmazonPt(dao, disc);
                } else {
                    computeAndUpdateSakuraPt(dao, disc);
                }

                session.flush();
                session.clear();
                Thread.yield();
            });
        });
    }

    public void updateDiscsTitleAndRelease() {
        LOGGER.info("[开始更新碟片信息]");

        Set<Disc> discs = new LinkedHashSet<>();

        dao.execute(session -> {
            @SuppressWarnings("unchecked")
            List<Sakura> sakuras = session.createCriteria(Sakura.class)
                    .add(Restrictions.eq("enabled", true))
                    .list();

            sakuras.forEach(sakura -> {
                discs.addAll(sakura.getDiscs());
            });
        });

        AtomicInteger count = new AtomicInteger(discs.size());
        LOGGER.info("[开始更新碟片信息][共{}个]", discs.size());

        discs.forEach(disc -> {
            service.createDiscTask(disc.getAsin(), task -> {
                if (task.isDone()) {
                    Node node = getNode(task.getDocument(), "Items", "Item", "ItemAttributes");
                    if (node != null) {
                        Document itemAttributes = node.getOwnerDocument();
                        String title = getText(itemAttributes, "Title");
                        String group = getText(itemAttributes, "ProductGroup");
                        String release = getText(itemAttributes, "ReleaseDate");
                        Objects.requireNonNull(title);
                        Objects.requireNonNull(group);
                        disc.setTitle(formatTitle(title));
                        if (disc.getDiscType() == Disc.DiscType.Auto) {
                            disc.setDiscType(getType(group, title));
                        }
                        if (release != null) {
                            LocalDate newDate = getReleaseDate(release);
                            LocalDate oldDate = disc.getReleaseDate();
                            if (oldDate == null || oldDate.isBefore(newDate)) {
                                disc.setReleaseDate(newDate);
                            }
                        }
                    }
                }
                count.decrementAndGet();
                if (count.get() % 5 == 0 || count.get() < 10) {
                    LOGGER.info("[正在更新碟片信息][还剩{}个][asin={}]", count.get(), task.getAsin());
                } else {
                    LOGGER.debug("[正在更新碟片信息][还剩{}个][asin={}]", count.get(), task.getAsin());
                }
                if (count.get() == 0) {
                    writeDiscInfos(discs);
                }
            });
        });
    }

    private void writeDiscInfos(Set<Disc> discs) {
        LOGGER.info("[正在写入碟片信息]");
        dao.execute(session -> {
            discs.forEach(disc -> {
                Disc theDisc = dao.get(Disc.class, disc.getId());
                theDisc.setTitle(disc.getTitle());
                theDisc.setReleaseDate(disc.getReleaseDate());
            });
        });
        LOGGER.info("[成功更新碟片信息]");
    }

}
