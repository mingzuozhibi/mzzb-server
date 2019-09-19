package mingzuozhibi.action;

import mingzuozhibi.support.Dao;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public class BaseController {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;
    protected Logger LOGGER;

    @Autowired
    protected Dao dao;

    public BaseController() {
        LOGGER = LoggerFactory.getLogger(this.getClass());
    }

    protected String objectResult(Object data) {
        JSONObject root = new JSONObject();
        root.put("success", true);
        root.put("data", data);
        return root.toString();
    }

    protected String objectResult(Object data, Object page) {
        JSONObject root = new JSONObject();
        root.put("success", true);
        root.put("data", data);
        root.put("page", page);
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

    private String getRemoteAddr() {
        HttpServletRequest request = getAttributes().getRequest();
        String xRealIp = request.getHeader("X-Real-IP");
        return xRealIp != null ? xRealIp : request.getRemoteAddr();
    }

    private String getCommon() {
        try {
            HttpServletRequest request = getAttributes().getRequest();
            String common = String.format("[%s][%s][%s][%s][%s][%s]",
                getRemoteAddr(), getUserName(), request.getMethod(),
                request.getRequestURI(), bodyString(request), paramString(request));
            return common.replace("{}", "{empty}");
        } catch (Exception e) {
            return String.format("[获取请求数据失败][error=%s]", e.getMessage())
                .replace("{}", "{empty}");
        }
    }

    private Object bodyString(HttpServletRequest request) {
        Object jsonRequestBody = request.getAttribute("JSON_REQUEST_BODY");
        if (jsonRequestBody instanceof String) {
            String body = (String) jsonRequestBody;
            if (body.length() > 200) {
                return body.substring(0, 200) + "...";
            } else {
                return body;
            }
        }
        return "NoBody";
    }

    private String paramString(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        request.getParameterMap().forEach((key, arr) -> {
            if (arr.length == 0) {
                builder.append(key).append('&');
            } else if (arr.length == 1) {
                builder.append(key).append('=').append(arr[0]).append('&');
            } else {
                builder.append(key).append('=').append(Arrays.toString(arr)).append('&');
            }
        });
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        } else {
            builder.append("NoParam");
        }
        return builder.toString();
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
