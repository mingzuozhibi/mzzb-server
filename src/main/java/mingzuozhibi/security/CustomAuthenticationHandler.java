package mingzuozhibi.security;

import mingzuozhibi.action.BaseController;
import mingzuozhibi.action.SessionController;
import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationHandler extends BaseController implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    @Autowired
    private Dao dao;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        dao.execute(session -> {
            dao.lookup(User.class, "username", getCurrentName())
                    .setLastLoggedIn(LocalDateTime.now().withNano(0));
        });
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}]登入成功:{}", getWebDetails().getRemoteAddress(), SessionController.getJSON(authentication));
        }
        responseObject(response, SessionController.getJSON(authentication));
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}]登入失败:[username={}]",
                    request.getRemoteAddr(), request.getAttribute("username"));
        }
        responseError(response, exception.getMessage());
    }
}
