package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.modules.remember.RememberRepository;
import com.mingzuozhibi.support.JsonArg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.ChecksUtils.*;
import static com.mingzuozhibi.commons.utils.ModifyUtils.logCreate;
import static com.mingzuozhibi.commons.utils.ModifyUtils.logUpdate;

@RestController
public class UserController extends BaseController2 {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RememberRepository rememberRepository;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/users", produces = MEDIA_TYPE)
    public String findAll() {
        List<User> users = userRepository.findAll();
        return dataResult(users);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/users/{id}", produces = MEDIA_TYPE)
    public String findById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(this::dataResult)
            .orElseGet(() -> paramNotExists("用户ID"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/users", produces = MEDIA_TYPE)
    public String createUser(@JsonArg("$.username") String username,
                             @JsonArg("$.password") String password,
                             @JsonArg(value = "$.enabled", defaults = "true") Boolean enabled) {
        Optional<String> checks = runChecks(
            checkNotEmpty(username, "用户名称"),
            checkIdentifier(username, "用户名称", 4, 20),
            checkNotEmpty(password, "用户密码"),
            checkMd5Encode(username, "用户密码", 32)
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        if (!userRepository.existsByUsername(username)) {
            return paramExists("用户名称");
        }
        User user = new User(username, password, enabled);
        userRepository.save(user);
        jmsMessage.info(logCreate("创建用户", user.getUsername(), gson.toJson(user)));
        return dataResult(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/api/users/{id}", produces = MEDIA_TYPE)
    public String updateUser(@PathVariable Long id,
                             @JsonArg("$.username") String username,
                             @JsonArg("$.password") String password,
                             @JsonArg("$.enabled") Boolean enabled) {
        Optional<String> checks = runChecks(
            checkNotEmpty(username, "用户名称"),
            checkIdentifier(username, "用户名称", 4, 20),
            checkMd5Encode(username, "用户密码", 32),
            checkSelected(enabled, "用户启用状态")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        Optional<User> byId = userRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("用户ID");
        }
        User user = byId.get();
        if (!Objects.equals(user.getUsername(), username)) {
            user.setUsername(username);
            jmsMessage.info(logUpdate("用户名称", user.getUsername(), username));
        }
        if (StringUtils.isNotEmpty(password) && !Objects.equals(user.getPassword(), password)) {
            user.setPassword(password);
            onChangePassword(user);
            jmsMessage.info(logUpdate("用户密码", "******", "******"));
        }
        if (user.isEnabled() != enabled) {
            user.setEnabled(enabled);
            jmsMessage.info(logUpdate("用户启用状态", user.isEnabled(), enabled));
        }
        return dataResult(user);
    }

    private void onChangePassword(User user) {
        rememberRepository.deleteByUser(user);
    }

}
