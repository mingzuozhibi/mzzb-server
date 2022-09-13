package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.configs.UserDetailsImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mingzuozhibi.commons.utils.sdk.ServletUtils.*;

public abstract class SessionUtils {

    public static final Set<GrantedAuthority> GUEST_AUTHORITIES = Stream.of("NONE")
        .map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

    public static Optional<Authentication> findAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static boolean isLogged(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(Predicate.isEqual("ROLE_BASIC"));
    }

    public static Authentication buildGuestAuthentication() {
        return new AnonymousAuthenticationToken(UUID.randomUUID().toString(), "Guest", GUEST_AUTHORITIES);
    }

    public static Authentication buildUserAuthentication(User user) {
        UserDetails userDetails = new UserDetailsImpl(user);
        var details = new WebAuthenticationDetails(getAttributes().getRequest());
        var token = new UsernamePasswordAuthenticationToken(
            userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        token.setDetails(details);
        return token;
    }

    public static String getSessionTokenFromHeader() {
        return getRequest().getHeader("session-token");
    }

    public static void setSessionTokenToHeader(String token) {
        getResponse().addHeader("session-token", token);
    }

    public static Long getSessionIdFromHttpSession() {
        return (Long) getSession(true).getAttribute("session-id");
    }

    public static void setSessionIdToHttpSession(Long sessionId) {
        getRequest().getSession().setAttribute("session-id", sessionId);
    }

}
