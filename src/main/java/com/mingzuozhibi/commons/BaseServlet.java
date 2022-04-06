package com.mingzuozhibi.commons;

import com.mingzuozhibi.modules.user.User;
import com.mingzuozhibi.security.UserDetailsImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseServlet {

    public static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    public static final Set<GrantedAuthority> GUEST_AUTHORITIES = Stream.of("NONE")
        .map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

    public static void responseText(HttpServletResponse response, String content) throws IOException {
        response.setContentType(MEDIA_TYPE);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    protected static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    protected static Authentication buildGuestAuthentication() {
        return new AnonymousAuthenticationToken(UUID.randomUUID().toString(), "Guest", GUEST_AUTHORITIES);
    }

    protected static Authentication buildUserAuthentication(User user) {
        UserDetails userDetails = new UserDetailsImpl(user);
        WebAuthenticationDetails details = new WebAuthenticationDetails(getHttpRequest());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
            userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        token.setDetails(details);
        return token;
    }

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static ServletRequestAttributes getAttributes() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
    }

    public static HttpServletRequest getHttpRequest() {
        return getAttributes().getRequest();
    }

    public static HttpServletResponse getHttpResponse() {
        return getAttributes().getResponse();
    }

    public static HttpSession getHttpSession() {
        return getHttpRequest().getSession();
    }

    public static String getUserName() {
        return getAuthentication().map(Authentication::getName).orElse("unknown");
    }

}
