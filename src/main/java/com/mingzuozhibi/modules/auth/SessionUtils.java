package com.mingzuozhibi.modules.auth;

import com.mingzuozhibi.commons.BaseServlet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.function.Predicate;

public abstract class SessionUtils extends BaseServlet {

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

    public static boolean isLogged(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(Predicate.isEqual("ROLE_BASIC"));
    }

}
