package com.mingzuozhibi.commons.utils;

public abstract class ThreadUtils {

    public static void startThread(Runnable runnable) {
        new Thread(runnable).start();
    }

}
