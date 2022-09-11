package com.mingzuozhibi.commons.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public abstract class MyTimeUtils {

    public static final ZoneId ZONE = ZoneId.systemDefault();

    public static final DateTimeFormatter fmtDateTime =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter fmtDate =
        DateTimeFormatter.ofPattern("yyyy/M/d");

    public static Instant toInstant(LocalDateTime time) {
        return time.atZone(ZONE).toInstant();
    }

    public static LocalDateTime ofInstant(Instant instant) {
        return instant.atZone(ZONE).toLocalDateTime();
    }

    public static long toEpochMilli(LocalDateTime time) {
        return toInstant(time).toEpochMilli();
    }

    public static LocalDateTime ofEpochMilli(long milli) {
        return ofInstant(Instant.ofEpochMilli(milli));
    }

}
