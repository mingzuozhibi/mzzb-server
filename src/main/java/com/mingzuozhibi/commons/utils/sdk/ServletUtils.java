package com.mingzuozhibi.commons.utils.sdk;

import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.*;
import java.util.Optional;

import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;

public abstract class ServletUtils {

    public static Optional<ServletRequestAttributes> findAttributes() {
        return Optional.ofNullable((ServletRequestAttributes) getRequestAttributes());
    }

    public static Optional<HttpServletRequest> findRequest() {
        return findAttributes().map(ServletRequestAttributes::getRequest);
    }

    public static Optional<HttpServletResponse> findResponse() {
        return findAttributes().map(ServletRequestAttributes::getResponse);
    }

    public static Optional<HttpSession> findSession(boolean create) {
        return findRequest().map(request -> request.getSession(create));
    }

    public static ServletRequestAttributes getAttributes() {
        return findAttributes().orElseThrow();
    }

    public static HttpServletRequest getRequest() {
        return findRequest().orElseThrow();
    }

    public static HttpServletResponse getResponse() {
        return findResponse().orElseThrow();
    }

    public static HttpSession getSession(boolean create) {
        return findSession(create).orElseThrow();
    }

}
