package mingzuozhibi.action;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BaseController {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;
    protected Logger LOGGER;

    public BaseController() {
        LOGGER = LoggerFactory.getLogger(this.getClass());
    }

    protected String objectResult(Object object) {
        JSONObject root = new JSONObject();
        root.put("success", true);
        root.put("data", object);
        return root.toString();
    }

    protected String errorMessage(String message) {
        JSONObject root = new JSONObject();
        root.put("success", false);
        root.put("message", message);
        return root.toString();
    }

    protected void responseObject(HttpServletResponse response, Object object) throws IOException {
        String content = objectResult(object);
        response.setContentType(MEDIA_TYPE);
        response.setContentLength(content.length());
        response.getWriter().write(content);
        response.flushBuffer();
    }

    protected void responseError(HttpServletResponse response, String message) throws IOException {
        String content = errorMessage(message);
        response.setContentType(MEDIA_TYPE);
        response.setContentLength(content.length());
        response.getWriter().write(content);
        response.flushBuffer();
    }

    protected Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    protected WebAuthenticationDetails getWebDetails() {
        return (WebAuthenticationDetails) getAuthentication().getDetails();
    }

    protected String getCurrentName() {
        return getAuthentication().getName();
    }

}
