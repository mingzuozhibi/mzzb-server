package mingzuozhibi.action;

import mingzuozhibi.persist.core.User;
import mingzuozhibi.persist.core.UserRepository;
import mingzuozhibi.support.JsonArg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Logger logger = LoggerFactory.getLogger(LoginController.class);

    @PostMapping("/api/login")
    public String ajaxLogin(@JsonArg("$.username") String username,
                            @JsonArg("$.password") String password) {
        logger.info("用户登入: username={}, password=******", username);
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails.getPassword().equals(password) && userDetails.isEnabled()) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, userDetails.getPassword(), userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                onLoginSuccess(username);
                logger.info("用户登入: 用户已成功登入, username={}", username);
                return "{\"success\": true}";
            } else {
                logger.info("用户登入: 未能成功登入, username={}", username);
            }
        } catch (UsernameNotFoundException ignored) {
            logger.info("用户登入: 未找到该用户, username={}", username);
        }
        return "{\"success\": false}";
    }

    private void onLoginSuccess(String username) {
        User user = userRepository.findByUsername(username);
        user.setLastLoggedin(new Date());
        userRepository.save(user);
    }

}
