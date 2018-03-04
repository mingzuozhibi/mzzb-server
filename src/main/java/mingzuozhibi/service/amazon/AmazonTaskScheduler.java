package mingzuozhibi.service.amazon;

import mingzuozhibi.persist.disc.Disc;
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
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class AmazonTaskScheduler {

    enum AmazonFetchStatus {
        waitingForUpdate, startFullUpdate
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonTaskScheduler.class);

    private AmazonFetchStatus amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;

    @Autowired
    private AmazonTaskService service;

    @Autowired
    private Dao dao;

    public void fetchData() {
        if (amazonFetchStatus == AmazonFetchStatus.waitingForUpdate) {
            checkAmazonHotData();
        }
    }

    private void checkAmazonHotData() {
        LOGGER.debug("[开始检测Amzon(Hot)数据]");
        Set<Disc> discs = new LinkedHashSet<>();
        dao.execute(session -> {
            findActiveSakura(session).forEach(sakura -> {
                findAmazonDiscs(sakura).limit(5).forEach(discs::add);
            });
        });
        LOGGER.debug("[正在检测Amzon(Hot)数据][共{}个]", discs.size());
        AtomicInteger updateCount = new AtomicInteger(discs.size());
        discs.forEach(disc -> {
            service.createRankTask(disc.getAsin(), checkHot(updateCount, disc));
        });
    }

    private Consumer<AmazonTask> checkHot(AtomicInteger updateCount, Disc disc) {
        return task -> {
            AtomicInteger newRank = new AtomicInteger();
            getRank(task).ifPresent(rank -> {
                newRank.set(rank);
                if (!rank.equals(disc.getThisRank())) {
                    startFullUpdate();
                }
            });
            LOGGER.debug("[正在检测Amzon(Hot)数据][{}->{}][还剩{}个]",
                    disc.getThisRank(), newRank.get(), updateCount.decrementAndGet());
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
            return disc.getUpdateType() == Disc.UpdateType.Amazon;
        });
    }

    private void startFullUpdate() {
        LOGGER.info("[开始更新Amzon(ALL)数据]");
        if (amazonFetchStatus == AmazonFetchStatus.waitingForUpdate) {
            amazonFetchStatus = AmazonFetchStatus.startFullUpdate;
            service.clearRankTasks();

            LocalDateTime startTime = LocalDateTime.now().withNano(0);

            LinkedHashSet<Disc> discs = new LinkedHashSet<>();
            AtomicInteger updateCount = new AtomicInteger(discs.size());
            LinkedHashMap<String, Integer> results = new LinkedHashMap<>();

            dao.execute(session -> {
                findActiveSakura(session).forEach(sakura -> {
                    findAmazonDiscs(sakura).forEach(discs::add);
                });
            });

            LOGGER.info("[正在更新Amzon(ALL)数据][共{}个]", discs.size());
            discs.stream().sorted().forEach(disc -> {
                service.createRankTask(disc.getAsin(), fullUpdate(startTime, discs, updateCount, results));
            });

        }
    }

    private Consumer<AmazonTask> fullUpdate(LocalDateTime startTime, LinkedHashSet<Disc> discs, AtomicInteger updateCount, LinkedHashMap<String, Integer> results) {
        return task -> {
            getRank(task).ifPresent(rank -> {
                results.put(task.getAsin(), rank);
            });
            LOGGER.info("[正在更新Amzon(ALL)数据][还剩{}个]", updateCount.decrementAndGet());
            if (updateCount.get() == 0) {
                finishTheUpdate(discs, startTime, results);
            }
        };
    }

    private void finishTheUpdate(LinkedHashSet<Disc> discs, LocalDateTime startTime, LinkedHashMap<String, Integer> results) {
        LOGGER.info("[正在写入Amzon(ALL)数据]]");
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
        LOGGER.info("[成功更新Amzon(ALL)数据]");
        amazonFetchStatus = AmazonFetchStatus.waitingForUpdate;
    }

}
