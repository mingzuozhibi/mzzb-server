package mingzuozhibi.security;

import mingzuozhibi.action.BaseController;
import mingzuozhibi.action.SessionController;
import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import org.json.JSONObject;
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

        JSONObject json = SessionController.getJSON(authentication);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}][登入成功][json={}]", request.getRemoteAddr(), json);
        }
        responseObject(response, json);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}][登入失败][username={}][message={}]",
                    request.getRemoteAddr(), request.getAttribute("username"), exception.getMessage());
        }
        responseError(response, exception.getMessage());
    }
}
