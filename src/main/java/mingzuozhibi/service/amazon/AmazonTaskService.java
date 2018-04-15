package mingzuozhibi.service.amazon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class AmazonTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonTaskService.class);

    @Autowired
    private Environment env;

    @Value("${amazon.account.count}")
    private int amazonAccountCount;

    private ArrayBlockingQueue<AmazonTaskFetcher> fetchers;
    private ExecutorService service;

    private ArrayBlockingQueue<AmazonTask> findTasks = new ArrayBlockingQueue<>(1000, true);
    private ArrayBlockingQueue<AmazonTask> rankTasks = new ArrayBlockingQueue<>(1000, true);
    private ArrayBlockingQueue<AmazonTask> doneTasks = new ArrayBlockingQueue<>(1000, true);

    private final Object taskLock = new Object();

    @PostConstruct
    public void init() {
        service = Executors.newFixedThreadPool(amazonAccountCount + 2);
        fetchers = new ArrayBlockingQueue<>(amazonAccountCount, true);

        for (int i = 1; i <= amazonAccountCount; i++) {
            String accessKey = env.getProperty("amazon.access." + i);
            String secretKey = env.getProperty("amazon.secret." + i);
            String associateTag = env.getProperty("amazon.userid." + i);
            fetchers.offer(new AmazonTaskFetcher(accessKey, secretKey, associateTag));
        }
        LOGGER.info("[Amazon更新服务][更新器已准备就绪，数量为：{}]", fetchers.size());
        service.submit(() -> {
            LOGGER.info("[Amazon更新服务][任务添加线程已就绪]");
            while (!service.isShutdown()) {
                takeFetcher().ifPresent(fetcher -> {
                    AmazonTask findTask = findTasks.poll();
                    if (findTask != null) {
                        submitTask(fetcher, findTask);
                        return;
                    }
                    AmazonTask rankTask = rankTasks.poll();
                    if (rankTask != null) {
                        submitTask(fetcher, rankTask);
                        return;
                    }
                    fetchers.offer(fetcher);
                    waitForTaskLock();
                });
            }
        });
        service.submit(() -> {
            LOGGER.info("[Amazon更新服务][任务执行线程已就绪]");
            while (!service.isShutdown()) {
                try {
                    AmazonTask doneTask = doneTasks.take();
                    doneTask.getConsumer().accept(doneTask);
                } catch (Exception e) {
                    LOGGER.warn("[Amazon更新服务][任务执行遇到异常]", e);
                }
            }
        });
    }

    private void submitTask(AmazonTaskFetcher fetcher, AmazonTask task) {
        service.submit(() -> {
            fetcher.fetch(task, result -> {
                doneTasks.offer(result);
            });
            fetchers.offer(fetcher);
        });
    }

    private Optional<AmazonTaskFetcher> takeFetcher() {
        try {
            return Optional.of(fetchers.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void waitForTaskLock() {
        try {
            synchronized (taskLock) {
                TimeUnit.SECONDS.timedWait(taskLock, 1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void nodifyTaskLock() {
        synchronized (taskLock) {
            taskLock.notify();
        }
    }

    public void createFindTask(String asin, Consumer<AmazonTask> consumer) {
        AmazonTask task = new AmazonTask(asin, "ItemAttributes,SalesRank", consumer, 3);
        findTasks.offer(task);
        nodifyTaskLock();
    }

    public void createRankTask(String asin, Consumer<AmazonTask> consumer) {
        AmazonTask task = new AmazonTask(asin, "SalesRank", consumer, 3);
        rankTasks.offer(task);
        nodifyTaskLock();
    }

    public void createDiscTask(String asin, Consumer<AmazonTask> consumer) {
        AmazonTask task = new AmazonTask(asin, "ItemAttributes", consumer, 3);
        rankTasks.offer(task);
        nodifyTaskLock();
    }

    public void infoStatus() {
        fetchers.forEach(fetcher -> {
            LOGGER.info("[Amazon更新服务][更新器tag:{}][创建时间:{}][运行时间:{}秒][运行效率:{}毫秒/任务]" +
                            "[总共任务:{}][失败任务:{}][总共连接:{}][503失败:{}][400失败:{}][其他失败:{}]",
                    fetcher.getAssociateTag(),
                    fetcher.getCreateTime(),
                    fetcher.getConnectTime() / 1000,
                    fetcher.computeCostPerTask(),
                    fetcher.getTotalTaskCount(),
                    fetcher.getErrorTaskCount(),
                    fetcher.getTotalConnectCount(),
                    fetcher.getEr503ConnectCount(),
                    fetcher.getEr400ConnectCount(),
                    fetcher.getErrorConnectCount());
        });
    }

    public void debugStatus() {
        LOGGER.debug("[Amazon更新服务][fetchers={}][findTasks={}][rankTasks={}][doneTasks={}]",
                fetchers.size(), findTasks.size(), rankTasks.size(), doneTasks.size());
        fetchers.forEach(fetcher -> {
            LOGGER.debug("[Amazon更新服务][更新器tag:{}][创建时间:{}][运行时间:{}秒][运行效率:{}毫秒/任务]" +
                            "[总共任务:{}][失败任务:{}][总共连接:{}][503失败:{}][400失败:{}][其他失败:{}]",
                    fetcher.getAssociateTag(),
                    fetcher.getCreateTime(),
                    fetcher.getConnectTime() / 1000,
                    fetcher.computeCostPerTask(),
                    fetcher.getTotalTaskCount(),
                    fetcher.getErrorTaskCount(),
                    fetcher.getTotalConnectCount(),
                    fetcher.getEr503ConnectCount(),
                    fetcher.getEr400ConnectCount(),
                    fetcher.getErrorConnectCount());
        });
    }

}
