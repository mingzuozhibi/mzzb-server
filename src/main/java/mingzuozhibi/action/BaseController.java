package mingzuozhibi.action;

import mingzuozhibi.persist.AutoLogin;
import mingzuozhibi.security.UserDetailsImpl;
import mingzuozhibi.support.Dao;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

public class BaseController {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;
    protected Logger LOGGER;

    @Autowired
    protected Dao dao;

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

    protected void responseText(HttpServletResponse response, String content) throws IOException {
        response.setContentType(MEDIA_TYPE);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    protected Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    protected String getUserName() {
        return getAuthentication().map(Authentication::getName).orElse("null");
    }

    protected ServletRequestAttributes getAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) attributes);

    }

    private String getCommon() {
        HttpServletRequest request = getAttributes().getRequest();
        String common = String.format("[%s][%s][%s][%s]",
                request.getRemoteAddr(), getUserName(), request.getMethod(), request.getRequestURI());
        return common.replace("{}", "\\{}");
    }

    protected void debugRequest(String format, Object... args) {
        LOGGER.debug(getCommon() + format, args);
    }

    protected void infoRequest(String format, Object... args) {
        LOGGER.info(getCommon() + format, args);
    }

    protected void warnRequest(String format, Object... args) {
        LOGGER.warn(getCommon() + format, args);
    }

    protected void errorRequest(String format, Object... args) {
        LOGGER.error(getCommon() + format, args);
    }

}
