package com.mingzuozhibi.modules.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void initAdminUser(String encodedPassword) {
        var user = userRepository.findByUsername("admin").orElseGet(() -> {
            log.info("初始化管理员用户");
            var admin = new User("admin", encodedPassword, true);
            admin.getRoles().add("ROLE_ADMIN");
            return userRepository.save(admin);
        });
        if (!encodedPassword.equals(user.getPassword())) {
            log.info("更新管理员密码");
            user.setPassword(encodedPassword);
        }
        if (!user.getRoles().contains("ROLE_BASIC")) {
            log.info("更新管理员权限");
            user.getRoles().add("ROLE_BASIC");
        }
        if (!user.getRoles().contains("ROLE_ADMIN")) {
            log.info("更新管理员权限");
            user.getRoles().add("ROLE_ADMIN");
        }
        if (!user.isEnabled()) {
            log.info("启用管理员用户");
            user.setEnabled(true);
        }
    }

}
