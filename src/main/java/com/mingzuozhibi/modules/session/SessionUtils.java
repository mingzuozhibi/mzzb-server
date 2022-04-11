package com.mingzuozhibi.modules.session;

import com.mingzuozhibi.utils.ServletUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.function.Predicate;

import static com.mingzuozhibi.modules.session.Authentications.getAuthentication;

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

    public static boolean isLogged(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(Predicate.isEqual("ROLE_BASIC"));
    }

    public static String getLoginName() {
        return getAuthentication().map(Principal::getName).orElse("*system*");
    }

}
