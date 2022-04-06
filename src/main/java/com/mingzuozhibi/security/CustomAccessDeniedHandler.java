package com.mingzuozhibi.security;

import com.mingzuozhibi.commons.BaseController;
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

@Component
public class CustomAccessDeniedHandler extends BaseController implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        if (LOGGER.isWarnEnabled()) {
            warnRequest("[匿名访问: message={}]", authException.getMessage());
        }
        responseText(response, errorMessage("你必须登入才能访问这些资源"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        if (LOGGER.isWarnEnabled()) {
            warnRequest("[越权访问: message={}]", accessDeniedException.getMessage());
        }
        responseText(response, errorMessage("你的权限不足以访问这些资源"));
    }
}
