package com.mingzuozhibi.utils;

import com.mingzuozhibi.commons.mylog.JmsLogger;

import java.time.Instant;

public abstract class ThreadUtils {

    public static void runWithDaemon(String name, JmsLogger bind, Callback callback) {
        bind.info("开始%s", name);
        Thread thread = new Thread(() -> {
            try {
                callback.call();
                bind.info("完成%s", name);
            } catch (Exception e) {
                bind.error("运行%s遇到错误: %s", name, e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public interface Callback {
        void call() throws Exception;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void waitSecond(Object lock, int seconds) {
        synchronized (lock) {
            long target = Instant.now().plusSeconds(seconds).toEpochMilli();
            while (true) {
                long timeout = target - Instant.now().toEpochMilli();
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

}
