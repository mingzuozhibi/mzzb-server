package com.mingzuozhibi.modules.session;

import com.mingzuozhibi.utils.ServletUtils;

public abstract class SessionUtils extends ServletUtils {

    public static String getTokenFromHeader() {
        return getHttpRequest().getHeader("session-token");
    }

    public static void setTokenToHeader(String token) {
        getHttpResponse().addHeader("session-token", token);
    }

    public static Long getSessionId() {
        return (Long) getHttpSession().getAttribute("session-id");
    }

    public static void setSessionId(Long sessionId) {
        getHttpSession().setAttribute("session-id", sessionId);
    }

}
