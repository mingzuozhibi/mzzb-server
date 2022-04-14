package com.mingzuozhibi.modules.session;

import com.mingzuozhibi.configs.UserDetailsImpl;
import com.mingzuozhibi.modules.user.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mingzuozhibi.utils.ServletUtils.getHttpRequest;

public abstract class Authentications {

    public static final Set<GrantedAuthority> GUEST_AUTHORITIES = Stream.of("NONE")
        .map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static Authentication buildGuestAuthentication() {
        return new AnonymousAuthenticationToken(UUID.randomUUID().toString(), "Guest", GUEST_AUTHORITIES);
    }

    public static Authentication buildUserAuthentication(User user) {
        UserDetails userDetails = new UserDetailsImpl(user);
        WebAuthenticationDetails details = new WebAuthenticationDetails(getHttpRequest());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
            userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        token.setDetails(details);
        return token;
    }

}
