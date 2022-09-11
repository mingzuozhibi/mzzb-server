package com.mingzuozhibi.commons.utils.sdk;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public abstract class RequestUtils {

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
