package mingzuozhibi.security;

import mingzuozhibi.action.BaseController;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint extends BaseController implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("[{}]越权访问:{}:{}", request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
        }
        responseError(response, authException.getMessage());
    }

}
