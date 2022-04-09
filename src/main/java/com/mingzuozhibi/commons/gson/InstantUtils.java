package com.mingzuozhibi.commons.gson;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public abstract class InstantUtils {

    public static Instant toInstant(LocalDateTime time) {
        return time.toInstant(ZoneOffset.of(ZoneId.systemDefault().getId()));
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
