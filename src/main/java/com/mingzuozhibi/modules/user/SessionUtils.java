package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseServlet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Predicate;

public abstract class SessionUtils extends BaseServlet {

    public static String getTokenFromHeader() {
        return getHttpRequest().getHeader("session-token");
    }

    public static void setTokenToHeader(String token) {
        getHttpResponse().addHeader("session-token", token);
    }

    public static Long getSessionIdFromHttpSession() {
        return (Long) getHttpSession().getAttribute("session-id");
    }

    public static void setSessionIdToHttpSession(Session session) {
        getHttpSession().setAttribute("session-id", session.getId());
    }

    public static JSONObject buildSession() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        JSONObject object = new JSONObject();
        object.put("userName", getUserName());
        object.put("isLogged", isLogged(authentication));
        object.put("userRoles", buildUserRoles(authentication));
        return object;
    }

    public static boolean isLogged(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(Predicate.isEqual("ROLE_BASIC"));
    }

    private static JSONArray buildUserRoles(Authentication authentication) {
        JSONArray userRoles = new JSONArray();
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .forEach(userRoles::put);
        return userRoles;
    }

}
