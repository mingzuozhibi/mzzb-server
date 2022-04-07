package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseController2;
import com.mingzuozhibi.commons.check.CheckResult;
import com.mingzuozhibi.commons.check.CheckUtils;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.support.JsonArg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.check.CheckHelper.*;
import static com.mingzuozhibi.commons.check.CheckUtils.doUpdate;
import static com.mingzuozhibi.commons.check.CheckUtils.paramNoExists;

@RestController
public class UserController extends BaseController2 {

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository2 sessionRepository2;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/users")
    public String findAll() {
        List<User> users = userRepository.findAll();
        return dataResult(users);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/users/{id}")
    public String findById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(this::dataResult)
            .orElseGet(() -> paramNoExists("用户ID"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/users")
    public String createUser(@JsonArg("$.username") String username,
                             @JsonArg("$.password") String password,
                             @JsonArg(value = "$.enabled", defaults = "true") Boolean enabled) {
        CheckResult checks = runAllCheck(
            checkNotEmpty(username, "用户名称"),
            checkIdentifier(username, "用户名称", 4, 20),
            checkNotEmpty(password, "用户密码"),
            checkIdentifier(username, "用户密码", 4, 20)
        );
        if (checks.hasError()) {
            return errorResult(checks.getError());
        }
        if (!userRepository.existsByUsername(username)) {
            return CheckUtils.paramBeExists("用户名称");
        }
        User user = new User(username, password, enabled);
        userRepository.save(user);
        jmsMessage.info(CheckUtils.doCreate("创建用户", user.getUsername(), gson.toJson(user)));
        return dataResult(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @JsonArg("$.username") String username,
                             @JsonArg("$.password") String password,
                             @JsonArg("$.enabled") Boolean enabled) {
        CheckResult checks = runAllCheck(
            checkNotEmpty(username, "用户名称"),
            checkIdentifier(username, "用户名称", 4, 20),
            checkMd5Encode(username, "用户密码", 32),
            checkSelected(enabled, "用户启用状态")
        );
        if (checks.hasError()) {
            return errorResult(checks.getError());
        }
        Optional<User> byId = userRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNoExists("用户ID");
        }
        User user = byId.get();
        if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            jmsMessage.info(doUpdate("用户名称", user.getUsername(), username));
        }
        if (StringUtils.isNotEmpty(password) && !Objects.equals(user.getPassword(), password)) {
            user.setPassword(password);
            onChangePassword(user);
            jmsMessage.info(doUpdate("用户密码", "******", "******"));
        }
        if (user.isEnabled() != enabled) {
            user.setEnabled(enabled);
            jmsMessage.info(doUpdate("用户启用状态", user.isEnabled(), enabled));
        }
        return dataResult(user);
    }

    private void onChangePassword(User user) {
        sessionRepository2.deleteByUser(user);
    }

}
