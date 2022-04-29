package com.mingzuozhibi.commons.utils;

import java.time.format.DateTimeFormatter;

public abstract class FormatUtils {

    public static final DateTimeFormatter fmtDateTime =
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss");

    public static final DateTimeFormatter fmtDate =
        DateTimeFormatter.ofPattern("yyyy/M/d");

    public static final DateTimeFormatter fmtTime =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final DateTimeFormatter fmtLog =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");


}
