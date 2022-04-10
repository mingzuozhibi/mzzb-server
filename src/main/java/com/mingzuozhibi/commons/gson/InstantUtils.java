package com.mingzuozhibi.commons.gson;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public abstract class InstantUtils {

    public static Instant toInstant(LocalDateTime time) {
        return ZonedDateTime.of(time, ZoneId.systemDefault()).toInstant();
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
