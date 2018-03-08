package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.service.amazon.AmazonTask;
import mingzuozhibi.service.amazon.AmazonTaskService;
import mingzuozhibi.service.amazon.DocumentReader;
import mingzuozhibi.support.Dao;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mingzuozhibi.persist.disc.Sakura.ViewType.SakuraList;
import static mingzuozhibi.service.AmazonScheduler.AmazonFetchStatus.*;

@Component
public class AmazonScheduler {

    enum AmazonFetchStatus {
        waitingForUpdate, startHotUpdate, startFullUpdate
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonScheduler.class);

    private transient AmazonFetchStatus amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
    private AtomicReference<LocalDateTime> fullUpdateTime = new AtomicReference<>();

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
        LOGGER.info("[开始检测Amazon(Hot)数据]");
        Set<Disc> discs = new LinkedHashSet<>();
        dao.execute(session -> {
            findActiveSakura(session).forEach(sakura -> {
                findAmazonDiscs(sakura).unordered().limit(5).forEach(discs::add);
            });
        });

        Set<Disc> hotDiscs = discs.stream().unordered().limit(10).collect(Collectors.toSet());
        if (hotDiscs.size() > 0) {
            LOGGER.debug("[开始检测Amazon(Hot)数据][共{}个]", hotDiscs.size());
            AtomicInteger updateCount = new AtomicInteger(hotDiscs.size());
            hotDiscs.forEach(disc -> {
                service.createRankTask(disc.getAsin(), checkHotCB(updateCount, disc));
            });
        } else {
            LOGGER.info("[结束检测Amazon(Hot)数据][未找到可以检测的Amazon(Hot)数据]]");
            amazonFetchStatus = waitingForUpdate;
        }
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
                if (amazonFetchStatus == startFullUpdate) {
                    LOGGER.info("[成功检测Amazon(Hot)数据，数据有变化]");
                    startFullUpdate();
                } else {
                    LOGGER.info("[成功检测Amazon(Hot)数据，数据无变化]");
                    service.debugStatus();
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

        updateFullUpdateTime();

        LinkedHashSet<Disc> discs = new LinkedHashSet<>();
        LinkedHashMap<String, Integer> results = new LinkedHashMap<>();

        Predicate<Disc> needUpdateDisc = disc -> {
            return disc.getModifyTime() == null || disc.getModifyTime().isBefore(fullUpdateTime.get());
        };

        dao.execute(session -> {
            findActiveSakura(session).forEach(sakura -> {
                findAmazonDiscs(sakura).filter(needUpdateDisc).forEach(discs::add);
            });
        });

        if (discs.size() > 0) {
            AtomicInteger updateCount = new AtomicInteger(discs.size());
            LOGGER.info("[正在更新Amazon(ALL)数据][共{}个]", discs.size());
            discs.stream().sorted().forEach(disc -> {
                service.createRankTask(disc.getAsin(), fullUpdateCB(discs, results, updateCount));
            });
        } else {
            LOGGER.info("[结束更新Amazon(ALL)数据][未找到可以更新的Amazon(ALL)数据]");
            amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
        }
    }

    private void updateFullUpdateTime() {
        LocalDateTime nowTime = LocalDateTime.now().withNano(0);
        if (fullUpdateTime.get() == null || nowTime.isAfter(fullUpdateTime.get().plusMinutes(20))) {
            fullUpdateTime.set(nowTime);
        } else {
            LOGGER.info("[补充更新Amazon(ALL)数据][上次更新时间:{}]", fullUpdateTime.get());
        }
    }

    private Consumer<AmazonTask> fullUpdateCB(Set<Disc> discs, Map<String, Integer> results, AtomicInteger updateCount) {
        return task -> {
            updateCount.decrementAndGet();
            getRank(task).ifPresent(rank -> {
                results.put(task.getAsin(), rank);
            });
            if (updateCount.get() % 5 == 0 || updateCount.get() < 10) {
                LOGGER.info("[正在更新Amazon(ALL)数据][还剩{}个][asin={}]", updateCount.get(), task.getAsin());
            } else {
                LOGGER.debug("[正在更新Amazon(ALL)数据][还剩{}个][asin={}]", updateCount.get(), task.getAsin());
            }
            if (updateCount.get() == 0) {
                finishTheUpdate(discs, results);
            }
        };
    }

    private void finishTheUpdate(Set<Disc> discs, Map<String, Integer> results) {
        LOGGER.info("[正在写入Amazon(ALL)数据]]");
        dao.execute(session -> {
            discs.forEach(disc -> {
                Integer rank = results.get(disc.getAsin());
                dao.refresh(disc);
                disc.setPrevRank(disc.getThisRank());
                if (rank != null) {
                    disc.setThisRank(rank);
                    disc.setUpdateTime(fullUpdateTime.get());
                }
                if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                    disc.setModifyTime(fullUpdateTime.get());
                }
            });
            dao.findAll(Sakura.class).stream()
                    .filter(sakura -> sakura.getViewType() != SakuraList)
                    .forEach(sakura -> {
                        sakura.setModifyTime(fullUpdateTime.get());
                    });
        });
        LOGGER.info("[成功更新Amazon(ALL)数据]");
        service.infoStatus();
        amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
    }

}
