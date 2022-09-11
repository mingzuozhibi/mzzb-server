package com.mingzuozhibi.commons.utils.sdk;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Stream;

import static com.mingzuozhibi.commons.utils.sdk.ServletUtils.findRequest;

public abstract class RealIpUtils {

    public static Optional<String> findRealIp() {
        return findRequest().map(request -> Stream.concat(
            getIpFromXFF(request).stream(),
            getIpFromXRI(request).stream()
        ).findFirst().orElseGet(request::getRemoteAddr));
    }

    private static Optional<String> getIpFromXFF(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-forwarded-for"))
            .flatMap(text -> Stream.of(text.split(",")).findFirst());
    }

    private static Optional<String> getIpFromXRI(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-real-ip"));
    }

}
