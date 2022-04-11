package com.mingzuozhibi.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class MyTimeUtils {

    public static Instant toInstant(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static LocalDateTime ofInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static long toEpochMilli(LocalDateTime time) {
        return toInstant(time).toEpochMilli();
    }

    public static LocalDateTime ofEpochMilli(long milli) {
        return ofInstant(Instant.ofEpochMilli(milli));
    }

}
