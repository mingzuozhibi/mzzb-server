package com.mingzuozhibi.utils;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class ServletUtils {

    public static void responseText(HttpServletResponse response, String content) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    public static ServletRequestAttributes getAttributes() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
    }

    public static HttpServletRequest getHttpRequest() {
        return getAttributes().getRequest();
    }

    public static HttpServletResponse getHttpResponse() {
        return getAttributes().getResponse();
    }

    public static HttpSession getHttpSession() {
        return getHttpRequest().getSession();
    }

}
