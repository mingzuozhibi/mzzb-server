package com.mingzuozhibi.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import static com.mingzuozhibi.commons.base.BaseSupport.errorResult;

@Slf4j
@Component
public class SecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        log.info("未登录访问：{} {}", request.getMethod(), request.getServletPath());
        logRequestHeaders(request);
        responseText(response, errorResult("你必须登入才能访问这些资源"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        log.info("无权限访问：{} {}", request.getMethod(), request.getServletPath());
        logRequestHeaders(request);
        responseText(response, errorResult("你的权限不足以访问这些资源"));
    }

    private void logRequestHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            log.debug("{}: {}", name, request.getHeader(name));
        }
    }

    public static void responseText(HttpServletResponse response, String content) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

}
