package mingzuozhibi.service.amazon;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.support.Dao;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mingzuozhibi.persist.disc.Sakura.ViewType.SakuraList;
import static mingzuozhibi.service.amazon.AmazonTaskScheduler.AmazonFetchStatus.*;

@Component
public class AmazonTaskScheduler {

    enum AmazonFetchStatus {
        waitingForUpdate, startHotUpdate, startFullUpdate
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonTaskScheduler.class);

    private transient AmazonFetchStatus amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;

    @Autowired
    private AmazonTaskService service;

    @Autowired
    private Dao dao;

    public void fetchData() {
        service.debugStatus();
        LOGGER.info("[Amazon调度器状态={}]", amazonFetchStatus);
        if (amazonFetchStatus == AmazonFetchStatus.waitingForUpdate) {
            amazonFetchStatus = startHotUpdate;
            checkAmazonHotData();
        }
    }

    private void checkAmazonHotData() {
        LOGGER.debug("[开始检测Amazon(Hot)数据]");
        Set<Disc> discs = new LinkedHashSet<>();
        dao.execute(session -> {
            findActiveSakura(session).forEach(sakura -> {
                findAmazonDiscs(sakura).sorted().limit(5).forEach(discs::add);
            });
        });

        Set<Disc> hotDiscs = discs.stream().sorted().limit(10).collect(Collectors.toSet());
        LOGGER.debug("[开始检测Amazon(Hot)数据][共{}个]", hotDiscs.size());
        AtomicInteger updateCount = new AtomicInteger(hotDiscs.size());
        hotDiscs.forEach(disc -> {
            service.createRankTask(disc.getAsin(), checkHotCB(updateCount, disc));
        });
    }

    private Consumer<AmazonTask> checkHotCB(AtomicInteger updateCount, Disc disc) {
        return task -> {
            updateCount.decrementAndGet();
            AtomicReference<Integer> newRank = new AtomicReference<>();
            getRank(task).ifPresent(rank -> {
                newRank.set(rank);
                if (!rank.equals(disc.getThisRank())) {
                    amazonFetchStatus = startFullUpdate;
                }
            });
            LOGGER.debug("[正在检测Amazon(Hot)数据][{}][{}->{}][还剩{}个][asin={}]",
                    Objects.equals(disc.getThisRank(), newRank.get()) ? "无变化" : "有变化",
                    disc.getThisRank(), newRank.get(), updateCount.get(), disc.getAsin());
            if (updateCount.get() == 0) {
                service.debugStatus();
                if (amazonFetchStatus == startFullUpdate) {
                    startFullUpdate();
                } else {
                    amazonFetchStatus = waitingForUpdate;
                }
            }
        };
    }

    private Optional<Integer> getRank(AmazonTask task) {
        if (task.isDone()) {
            String rankText = DocumentReader.getText(task.getDocument(), "Items", "Item", "SalesRank");
            return Optional.ofNullable(rankText).map(Integer::parseInt);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Sakura> findActiveSakura(Session session) {
        return (List<Sakura>) session.createCriteria(Sakura.class)
                .add(Restrictions.eq("enabled", true))
                .add(Restrictions.ne("key", "9999-99"))
                .list();
    }

    private Stream<Disc> findAmazonDiscs(Sakura sakura) {
        return sakura.getDiscs().stream().filter(disc -> {
            UpdateType updateType = disc.getUpdateType();
            return updateType == UpdateType.Amazon || updateType == UpdateType.Both;
        });
    }

    private void startFullUpdate() {
        LOGGER.info("[开始更新Amazon(ALL)数据]");

        LocalDateTime startTime = LocalDateTime.now().withNano(0);

        LinkedHashSet<Disc> discs = new LinkedHashSet<>();
        LinkedHashMap<String, Integer> results = new LinkedHashMap<>();

        dao.execute(session -> {
            findActiveSakura(session).forEach(sakura -> {
                findAmazonDiscs(sakura).forEach(discs::add);
            });
        });

        AtomicInteger updateCount = new AtomicInteger(discs.size());
        LOGGER.info("[正在更新Amazon(ALL)数据][共{}个]", discs.size());
        discs.stream().sorted().forEach(disc -> {
            service.createRankTask(disc.getAsin(), fullUpdateCB(startTime, discs, updateCount, results));
        });
    }

    private Consumer<AmazonTask> fullUpdateCB(LocalDateTime startTime, LinkedHashSet<Disc> discs, AtomicInteger updateCount, LinkedHashMap<String, Integer> results) {
        return task -> {
            updateCount.decrementAndGet();
            getRank(task).ifPresent(rank -> {
                results.put(task.getAsin(), rank);
            });
            if (updateCount.get() % 5 == 0 || updateCount.get() < 10) {
                LOGGER.info("[正在更新Amazon(ALL)数据][还剩{}个]", updateCount.get());
            } else {
                LOGGER.debug("[正在更新Amazon(ALL)数据][还剩{}个]", updateCount.get());
            }
            if (updateCount.get() == 0) {
                finishTheUpdate(discs, startTime, results);
            }
        };
    }

    private void finishTheUpdate(LinkedHashSet<Disc> discs, LocalDateTime startTime, LinkedHashMap<String, Integer> results) {
        LOGGER.info("[正在写入Amazon(ALL)数据]]");
        discs.forEach(disc -> {
            for (int i = 0; i < 3; i++) {
                try {
                    Integer rank = results.get(disc.getAsin());
                    dao.refresh(disc);
                    disc.setPrevRank(disc.getThisRank());
                    if (rank != null) {
                        disc.setThisRank(rank);
                    }
                    disc.setModifyTime(startTime);
                    dao.update(disc);
                    break;
                } catch (DataException ignore) {
                }
            }
        });
        dao.findAll(Sakura.class).stream()
                .filter(sakura -> sakura.getViewType() != SakuraList)
                .forEach(sakura -> {
                    sakura.setModifyTime(startTime);
                });
        LOGGER.info("[成功更新Amazon(ALL)数据]");
        service.infoStatus();
        amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
    }

}
