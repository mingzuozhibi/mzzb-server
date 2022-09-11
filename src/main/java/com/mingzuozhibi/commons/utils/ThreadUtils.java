package com.mingzuozhibi.commons.utils;

import com.mingzuozhibi.commons.logger.Logger;

import java.time.Instant;
import java.util.Random;
import java.util.function.Supplier;

import static com.mingzuozhibi.commons.utils.LoggerUtils.logError;

public abstract class ThreadUtils {

    public static void runWithAction(Logger logger, String action, Callback callback) {
        try {
            callback.call();
        } catch (Exception e) {
            logError(logger, e, "%s失败".formatted(action));
        }
    }

    public static void runWithDaemon(Logger logger, String action, Callback callback) {
        var thread = new Thread(() -> {
            runWithAction(logger, action, callback);
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void logWithAction(Logger logger, String action, Supplier<Long> supplier) {
        try {
            logger.notify("开始%s".formatted(action));
            var count = supplier.get();
            logger.success("%s成功：共%d个".formatted(action, count));
        } catch (Exception e) {
            logError(logger, e, "%s失败".formatted(action));
        }
    }

    public static void logWithDaemon(Logger logger, String action, Supplier<Long> supplier) {
        var thread = new Thread(() -> {
            logWithAction(logger, action, supplier);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("RedundantThrows")
    public interface Callback {
        void call() throws Exception;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void waitSecond(Object lock, int seconds) {
        synchronized (lock) {
            var target = Instant.now().plusSeconds(seconds).toEpochMilli();
            while (true) {
                var timeout = target - Instant.now().toEpochMilli();
                if (timeout > 0) {
                    try {
                        lock.wait(timeout);
                        break;
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void notifyAll(Object lock) {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static void sleepSeconds(int seconds) {
        sleepMillis(seconds * 1000L);
    }

    public static void sleepSeconds(int minSeconds, int maxSeconds) {
        sleepSeconds(new Random().nextInt(minSeconds, maxSeconds + 1));
    }

}
