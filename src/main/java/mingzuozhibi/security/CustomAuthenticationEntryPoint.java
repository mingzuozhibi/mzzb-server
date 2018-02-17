package mingzuozhibi.security;

import mingzuozhibi.action.BaseController;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint extends BaseController implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("[{}][匿名访问][method={}][uri={}]",
                    request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
        }
        String message = authException.getMessage();
        if (LocaleContextHolder.getLocale().getLanguage().equals("zh")) {
            if (message.equals("Full authentication is required to access this resource")) {
                message = "你必须登入才能访问这些资源";
            }
        }
        responseError(response, message);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("[{}][越权访问][method={}][uri={}]",
                    request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
        }
        responseError(response, accessDeniedException.getMessage());
    }
}
