package com.mingzuozhibi.commons.utils;

import com.mingzuozhibi.commons.logger.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

@Slf4j
public abstract class LoggerUtils {

    public static Optional<String> findRealIp() {
        return getRequest().map(request -> Stream.concat(
                getIpFromXFF(request).stream(),
                getIpFromXRI(request).stream()
            ).findFirst()
            .orElseGet(request::getRemoteAddr));
    }

    private static Optional<String> getIpFromXFF(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-forwarded-for"))
            .flatMap(text -> Stream.of(text.split(",")).findFirst());
    }

    private static Optional<String> getIpFromXRI(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-real-ip"));
    }

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
        getRequest().ifPresent(request -> log.debug(formatRequest(request)));
    }

    public static Optional<HttpServletRequest> getRequest() {
        return Optional.ofNullable(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()))
            .map(ServletRequestAttributes::getRequest);
    }

    public static String formatRequest(HttpServletRequest request) {
        var params = request.getParameterMap().entrySet().stream()
            .map(e -> e.getKey() + "=" + printValue(e.getValue()))
            .collect(Collectors.joining(", ", "{", "}"));
        var headers = streamOf(request.getHeaderNames())
            .map(key -> key + "=" + printValue(request.getHeaders(key)))
            .collect(Collectors.joining(", ", "{", "}"));
        return "Request: uri=%s %s, params=%s, headers=%s".formatted(
            request.getMethod(), request.getRequestURI(), params, headers
        );
    }

    private static <E> Stream<E> streamOf(Enumeration<E> enumeration) {
        Builder<E> builder = Stream.builder();
        while (enumeration.hasMoreElements()) {
            builder.add(enumeration.nextElement());
        }
        return builder.build();
    }

    private static String printValue(Enumeration<String> headers) {
        return printValue(streamOf(headers).toList().toArray(new String[0]));
    }

    private static String printValue(String[] value) {
        return value.length == 1 ? value[0] : Arrays.toString(value);
    }

}
