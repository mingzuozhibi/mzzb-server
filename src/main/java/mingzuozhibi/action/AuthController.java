package mingzuozhibi.action;

import mingzuozhibi.persist.core.User;
import mingzuozhibi.persist.core.UserRepository;
import mingzuozhibi.support.JsonArg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/api/auth/login")
    public String login(@JsonArg("$.username") String username,
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
                return "{\"success\": false}";
            }
        } catch (UsernameNotFoundException ignored) {
            logger.info("用户登入: 未找到该用户, username={}", username);
            return "{\"success\": false}";
        }
    }

    @PostMapping("/api/auth/logout")
    public String logout() {
        logger.info("用户登出: 正在检测登入状态");

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            context.setAuthentication(null);
            logger.info("用户登出: 用户已成功登出, username={}", authentication.getName());
            return "{\"success\": true}";
        } else {
            logger.info("用户登出: 未检测到已登入状态");
            return "{\"success\": false}";
        }
    }

    @GetMapping("/api/auth/status")
    public String status() {
        logger.info("状态获取: 正在检测登入状态");

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            logger.info("状态获取: 检测到已登入用户, username={}", username);
            return "{\"success\": true, \"username\": \"" + username + "\"}";
        } else {
            logger.info("状态获取: 未检测到已登入状态");
            return "{\"success\": false}";
        }
    }

    private void onLoginSuccess(String username) {
        User user = userRepository.findByUsername(username);
        user.setLastLoggedin(new Date());
        userRepository.save(user);
    }

}
