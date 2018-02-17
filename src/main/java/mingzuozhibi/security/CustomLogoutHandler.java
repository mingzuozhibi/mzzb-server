package mingzuozhibi.security;

import mingzuozhibi.action.BaseController;
import mingzuozhibi.action.SessionController;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLogoutHandler extends BaseController implements LogoutHandler, LogoutSuccessHandler {
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}][登出成功][username={}]", request.getRemoteAddr(), authentication.getName());
        }
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        responseText(response, objectResult(SessionController.getJSON(null)));
    }
}
