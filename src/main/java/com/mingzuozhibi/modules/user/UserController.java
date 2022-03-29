package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.action.BaseController;
import com.mingzuozhibi.commons.check.CheckResult;
import com.mingzuozhibi.commons.check.CheckUtils;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.mingzuozhibi.commons.check.CheckHelper.*;
import static com.mingzuozhibi.commons.check.CheckUtils.doUpdate;
import static com.mingzuozhibi.commons.check.CheckUtils.paramNoExists;

@RestController
public class UserController extends BaseController {

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository2 sessionRepository2;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/users")
    public String findAll() {
        List<User> users = userRepository.findAll();
        return objectResult(UserUtils.buildUsers(users));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/users/{id}")
    public String findById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(user -> objectResult(user.toJSON()))
            .orElseGet(() -> paramNoExists("用户ID"));
    }

    private static class CreateForm {
        public String username;
        public String password;
        public boolean enabled = true;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/users", produces = MEDIA_TYPE)
    public String createUser(@RequestBody CreateForm form) {
        CheckResult checks = runAllCheck(
            checkNotEmpty(form.username, "用户名称"),
            checkIdentifier(form.username, "用户名称", 4, 20),
            checkNotEmpty(form.password, "用户密码"),
            checkIdentifier(form.username, "用户密码", 4, 20)
        );
        if (checks.hasError()) {
            return errorMessage(checks.getError());
        }
        if (!userRepository.existsByUsername(form.username)) {
            return CheckUtils.paramBeExists("用户名称");
        }
        User user = new User(form.username, form.password, form.enabled);
        userRepository.save(user);
        JSONObject result = UserUtils.buildUser(user);
        jmsMessage.info(CheckUtils.doCreate("创建用户", user.getUsername(), result));
        return objectResult(result);
    }

    private static class UpdateForm {
        public String username;
        public String password;
        public Boolean enabled;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/api/users/{id}", produces = MEDIA_TYPE)
    public String updateUser(@PathVariable Long id, @RequestBody UpdateForm form) {
        CheckResult checks = runAllCheck(
            checkNotEmpty(form.username, "用户名称"),
            checkIdentifier(form.username, "用户名称", 4, 20),
            checkIdentifierOrEmpty(form.username, "用户密码", 4, 20),
            checkSelected(form.enabled, "用户启用状态")
        );
        if (checks.hasError()) {
            return errorMessage(checks.getError());
        }
        Optional<User> byId = userRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNoExists("用户ID");
        }
        User user = byId.get();
        if (!user.getUsername().equals(form.username)) {
            user.setUsername(form.username);
            jmsMessage.info(doUpdate("用户名称", user.getUsername(), form.username));
        }
        if (StringUtils.isNotEmpty(form.password))
            if (!user.getPassword().equals(form.password)) {
                user.setPassword(form.password);
                onChangePassword(user);
                jmsMessage.info(doUpdate("用户密码", "******", "******"));
            }
        if (user.isEnabled() != form.enabled) {
            user.setEnabled(form.enabled);
            jmsMessage.info(doUpdate("用户启用状态", user.isEnabled(), form.enabled));
        }
        return objectResult(UserUtils.buildUser(user));
    }

    private void onChangePassword(User user) {
        sessionRepository2.deleteByUser(user);
    }

}
