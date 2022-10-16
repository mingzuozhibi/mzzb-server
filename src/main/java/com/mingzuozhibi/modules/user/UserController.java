package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static com.mingzuozhibi.commons.base.BaseController.DEFAULT_TYPE;
import static com.mingzuozhibi.support.ChecksUtils.*;
import static com.mingzuozhibi.support.ModifyUtils.*;

@LoggerBind(Name.SERVER_USER)
@Transactional
@RestController
@RequestMapping(produces = DEFAULT_TYPE)
public class UserController extends BaseController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RememberRepository rememberRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/users")
    public String findAll() {
        return dataResult(userRepository.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/users/{id}")
    public String findById(@PathVariable Long id) {
        var byId = userRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("用户ID");
        }
        return dataResult(byId.get());
    }

    @Setter
    private static class EntityForm {
        private String username;
        private String password;
        private Boolean enabled;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/users")
    public String createUser(@RequestBody EntityForm form) {
        var checks = runChecks(
            checkNotEmpty(form.username, "用户名称"),
            checkStrMatch(form.username, "用户名称", "[A-Za-z0-9_]{4,20}"),
            checkNotEmpty(form.password, "用户密码"),
            checkStrMatch(form.password, "用户密码", "[0-9a-f]{32}")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        if (userRepository.existsByUsername(form.username)) {
            return paramExists("用户名称");
        }
        var user = new User(form.username, form.password, form.enabled);
        userRepository.save(user);
        bind.success(logCreate("用户", user.getUsername(), gson.toJson(user)));
        return dataResult(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestBody EntityForm form) {
        var checks = runChecks(
            checkNotEmpty(form.username, "用户名称"),
            checkStrMatch(form.username, "用户名称", "[A-Za-z0-9_]{4,20}"),
            checkStrMatch(form.password, "用户密码", "[0-9a-f]{32}"),
            checkNotEmpty(form.enabled, "启用状态")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        var byId = userRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("用户ID");
        }
        var user = byId.get();
        if (!Objects.equals(user.getUsername(), form.username)) {
            bind.notify(logUpdate("用户名称", user.getUsername(), form.username, user.getUsername()));
            user.setUsername(form.username);
        }
        if (StringUtils.isNotEmpty(form.password) && !Objects.equals(user.getPassword(), form.password)) {
            bind.notify(logUpdate("用户密码", "******", "******", user.getUsername()));
            user.setPassword(form.password);
            onChangePassword(user);
        }
        if (user.isEnabled() != form.enabled) {
            bind.notify(logUpdate("启用状态", user.isEnabled(), form.enabled, user.getUsername()));
            user.setEnabled(form.enabled);
        }
        return dataResult(user);
    }

    private void onChangePassword(User user) {
        rememberRepository.deleteByUser(user);
    }

}
