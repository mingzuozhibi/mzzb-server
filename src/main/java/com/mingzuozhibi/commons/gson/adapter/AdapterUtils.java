package com.mingzuozhibi.commons.gson.adapter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class AdapterUtils {

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
