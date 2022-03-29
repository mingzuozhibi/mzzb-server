package com.mingzuozhibi.action;

import com.mingzuozhibi.persist.user.AutoLogin;
import com.mingzuozhibi.persist.user.User;
import com.mingzuozhibi.support.JsonArg;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController extends BaseController {

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/users", produces = MEDIA_TYPE)
    public String findAll() {
        JSONArray result = new JSONArray();
        dao.findAll(User.class).forEach(user -> {
            result.put(user.toJSON());
        });
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/users", produces = MEDIA_TYPE)
    public String addOne(
        @JsonArg String username,
        @JsonArg String password,
        @JsonArg(defaults = "true") boolean enabled) {

        if (dao.lookup(User.class, "username", username) != null) {
            return errorMessage("同户名已存在");
        }

        User user = new User(username, password, enabled);
        dao.save(user);

        JSONObject result = user.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[创建用户成功][用户信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/users/{id}", produces = MEDIA_TYPE)
    public String getOne(@PathVariable Long id) {
        User user = dao.get(User.class, id);
        if (user == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取用户失败][指定的用户Id不存在][Id={}]", id);
            }
            return errorMessage("指定的用户Id不存在");
        }
        return objectResult(user.toJSON());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/api/users/{id}", produces = MEDIA_TYPE)
    public String setOne(
        @PathVariable Long id,
        @JsonArg String username,
        @JsonArg String password,
        @JsonArg boolean enabled) {

        User user = dao.get(User.class, id);
        if (user == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑用户失败][指定的用户Id不存在][Id={}]", id);
            }
            return errorMessage("指定的用户Id不存在");
        }

        if (LOGGER.isInfoEnabled()) {
            JSONObject before = user.toJSON();
            infoRequest("[编辑用户开始][修改前={}]", before);
        }

        user.setUsername(username);
        user.setEnabled(enabled);
        if (StringUtils.isNotEmpty(password)) {
            user.setPassword(password);
            cleanAutoLogin(user);
        }

        JSONObject result = user.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑用户成功][修改后={}]", result);
        }
        return objectResult(result);
    }

    private void cleanAutoLogin(User user) {
        List<AutoLogin> autoLogins = dao.findBy(AutoLogin.class, "user", user);
        autoLogins.forEach(autoLogin -> {
            if (LOGGER.isDebugEnabled()) {
                debugRequest("[用户修改密码][清理自动登入数据][数据Id={}]", autoLogin.getId());
            }
            dao.delete(autoLogin);
        });
    }

}
