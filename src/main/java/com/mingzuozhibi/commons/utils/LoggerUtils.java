package com.mingzuozhibi.commons.utils;

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

    public static void logRequestIfExists() {
        Optional.ofNullable(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()))
            .map(ServletRequestAttributes::getRequest).ifPresent(LoggerUtils::printRequest);
    }

    public static void printRequest(HttpServletRequest request) {
        String params = request.getParameterMap().entrySet().stream()
            .map(e -> e.getKey() + "=" + printValue(e.getValue()))
            .collect(Collectors.joining(", ", "{", "}"));
        String headers = streamOf(request.getHeaderNames())
            .map(key -> key + "=" + printValue(request.getHeaders(key)))
            .collect(Collectors.joining(", ", "{", "}"));
        log.debug("Request: uri=%s %s, params=%s, headers=%s".formatted(
            request.getMethod(), request.getRequestURI(), params, headers
        ));
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
