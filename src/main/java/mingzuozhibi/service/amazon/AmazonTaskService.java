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

    private ArrayBlockingQueue<AmazonTask> discTasks = new ArrayBlockingQueue<>(1000, true);
    private ArrayBlockingQueue<AmazonTask> rankTasks = new ArrayBlockingQueue<>(1000, true);
    private ArrayBlockingQueue<AmazonTask> doneTasks = new ArrayBlockingQueue<>(1000, true);

    private final Object taskLock = new Object();
    private final Object doneLock = new Object();

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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[AmazonTask调度器][更新器准备就绪，总数量为：{}]", fetchers.size());
        }
        service.submit(() -> {
            LOGGER.info("[AmazonTask调度器][任务添加线程已就绪]");
            while (!service.isShutdown()) {
                takeFetcher().ifPresent(fetcher -> {
                    AmazonTask discTask = discTasks.poll();
                    if (discTask != null) {
                        submitTask(fetcher, discTask);
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
            LOGGER.info("[AmazonTask调度器][任务执行线程已就绪]");
            while (!service.isShutdown()) {
                try {
                    AmazonTask doneTask = doneTasks.take();
                    doneTask.getConsumer().accept(doneTask);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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

    public void createDiscTask(String asin, Consumer<AmazonTask> consumer) {
        AmazonTask task = new AmazonTask(asin, "ItemAttributes,SalesRank", consumer, 3);
        discTasks.offer(task);
        nodifyTaskLock();
    }

    public void createRankTask(String asin, Consumer<AmazonTask> consumer) {
        AmazonTask task = new AmazonTask(asin, "SalesRank", consumer, 3);
        rankTasks.offer(task);
        nodifyTaskLock();
    }

    public void clearRankTasks() {
        rankTasks.clear();
    }

}
