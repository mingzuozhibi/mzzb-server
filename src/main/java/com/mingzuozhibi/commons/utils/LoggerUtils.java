package com.mingzuozhibi.commons.utils;

import com.mingzuozhibi.commons.logger.Logger;
import lombok.extern.slf4j.Slf4j;

import static com.mingzuozhibi.commons.utils.sdk.RequestUtils.formatRequest;
import static com.mingzuozhibi.commons.utils.sdk.ServletUtils.findRequest;

@Slf4j
public abstract class LoggerUtils {

    public static void logWarn(Logger logger, boolean isDebug, String formatted) {
        if (isDebug) {
            logger.debug(formatted);
        } else {
            logger.warning(formatted);
        }
    }

    public static void logError(Logger logger, Exception e, String formatted) {
        logger.error("%s：%s".formatted(formatted, e.toString()));
        log.warn(formatted, e);
    }

    public static void logRequestIfExists() {
        findRequest().ifPresent(request -> log.debug(formatRequest(request)));
    }

}
