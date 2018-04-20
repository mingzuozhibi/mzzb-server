package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.persist.disc.Sakura.ViewType;
import mingzuozhibi.service.amazon.AmazonTask;
import mingzuozhibi.service.amazon.AmazonTaskService;
import mingzuozhibi.support.Dao;
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

import static mingzuozhibi.service.amazon.DocumentReader.getText;

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
            amazonFetchStatus = AmazonFetchStatus.startHotUpdate;
            checkAmazonHotData();
        }
    }

    private void checkAmazonHotData() {
        LOGGER.info("[开始检测Amazon(Hot)数据]");

        Set<Disc> discs = findNeedUpdateDiscs().stream()
                .unordered().limit(10).collect(Collectors.toSet());

        if (discs.size() > 0) {
            LOGGER.debug("[开始检测Amazon(Hot)数据][共{}个]", discs.size());
            AtomicInteger updateCount = new AtomicInteger(discs.size());
            discs.forEach(disc -> {
                service.createRankTask(disc.getAsin(), checkHotCB(updateCount, disc));
            });
        } else {
            LOGGER.info("[结束检测Amazon(Hot)数据][未找到可以检测的Amazon(Hot)数据]]");
            amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
        }
    }

    private Set<Disc> findNeedUpdateDiscs() {
        LocalDateTime needUpdate = LocalDateTime.now().minusMinutes(20);
        Set<Disc> discs = new LinkedHashSet<>();
        dao.execute(session -> {
            dao.findBy(Sakura.class, "enabled", true).forEach(sakura -> {
                sakura.getDiscs().stream().filter(disc -> {
                    UpdateType updateType = disc.getUpdateType();
                    return updateType == UpdateType.Amazon || updateType == UpdateType.Both;
                }).filter(disc -> {
                    LocalDateTime lastUpdate = fullUpdateTime.get();
                    LocalDateTime lastModify = disc.getModifyTime();
                    if (lastUpdate == null || lastUpdate.isBefore(needUpdate)) return true;
                    if (lastModify == null || lastModify.isBefore(lastUpdate)) return true;
                    return false;
                }).forEach(discs::add);
            });
        });
        return discs;
    }

    private Consumer<AmazonTask> checkHotCB(AtomicInteger updateCount, Disc disc) {
        return task -> {
            updateCount.decrementAndGet();
            AtomicReference<Integer> newRank = new AtomicReference<>();
            if (task.isDone()) {
                newRank.set(getRank(task));
                boolean rankNotChange = Objects.equals(disc.getThisRank(), newRank.get());
                if (!rankNotChange) {
                    amazonFetchStatus = AmazonFetchStatus.startFullUpdate;
                }
                LOGGER.info("[正在检测Amazon(Hot)数据][{}][{}->{}][还剩{}个][asin={}]",
                        rankNotChange ? "无变化" : "有变化",
                        disc.getThisRank(), newRank.get(), updateCount.get(), disc.getAsin());
            } else {
                LOGGER.info("[正在检测Amazon(Hot)数据][检测失败跳过][还剩{}个][asin={}]",
                        updateCount.get(), disc.getAsin());
            }
            if (updateCount.get() == 0) {
                if (amazonFetchStatus == AmazonFetchStatus.startFullUpdate) {
                    LOGGER.info("[成功检测Amazon(Hot)数据，数据有变化]");
                    startFullUpdate();
                } else {
                    LOGGER.info("[成功检测Amazon(Hot)数据，数据无变化]");
                    service.debugStatus();
                    amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
                }
            }
        };
    }

    private Integer getRank(AmazonTask task) {
        String rankText = getText(task.getDocument(), "Items", "Item", "SalesRank");
        return rankText == null ? Integer.valueOf(0) : Integer.valueOf(rankText);
    }

    private void startFullUpdate() {
        LOGGER.info("[开始更新Amazon(ALL)数据]");

        updateFullUpdateTime();

        Map<String, Integer> results = new LinkedHashMap<>();

        Set<Disc> discs = findNeedUpdateDiscs();

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
        LocalDateTime canFull = nowTime.minusMinutes(20);
        if (fullUpdateTime.get() == null || fullUpdateTime.get().isBefore(canFull)) {
            fullUpdateTime.set(nowTime);
        } else {
            LOGGER.info("[补充更新Amazon(ALL)数据][上次更新时间:{}]", fullUpdateTime.get());
        }
    }

    private Consumer<AmazonTask> fullUpdateCB(Set<Disc> discs, Map<String, Integer> results, AtomicInteger updateCount) {
        return task -> {
            updateCount.decrementAndGet();
            if (task.isDone()) {
                results.put(task.getAsin(), getRank(task));
            }
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
                    .filter(sakura -> sakura.getViewType() != ViewType.SakuraList)
                    .forEach(sakura -> {
                        sakura.setModifyTime(fullUpdateTime.get());
                    });
        });
        LOGGER.info("[成功更新Amazon(ALL)数据]");
        service.infoStatus();
        amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
    }

}
