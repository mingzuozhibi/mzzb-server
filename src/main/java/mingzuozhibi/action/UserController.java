package mingzuozhibi.action;

import mingzuozhibi.persist.AutoLogin;
import mingzuozhibi.persist.User;
import mingzuozhibi.support.JsonArg;
import org.json.JSONArray;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController extends BaseController {

    @Transactional
    @GetMapping(value = "/api/admin/users", produces = MEDIA_TYPE)
    public String listAdminUser() {
        JSONArray array = new JSONArray();
        dao.findAll(User.class).forEach(user -> {
            array.put(user.toJSON());
        });

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[size={}]", array.length());
        }
        return objectResult(array);
    }

    @Transactional
    @PostMapping(value = "/api/admin/users", produces = MEDIA_TYPE)
    public String saveAdminUser(
            @JsonArg("$.username") String username,
            @JsonArg("$.password") String password) {
        if (dao.lookup(User.class, "username", username) != null) {
            return errorMessage("用户名已被注册");
        }
        User user = new User(username, password);
        dao.save(user);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[json={}]", user.toJSON());
        }
        return objectResult(user.toJSON());
    }

    @Transactional
    @PostMapping(value = "/api/admin/users/{id}", produces = MEDIA_TYPE)
    public String editAdminUser(
            @PathVariable("id") Long id,
            @JsonArg("$.username") String username,
            @JsonArg("$.password") String password,
            @JsonArg("$.enabled") boolean enabled) {
        User user = dao.get(User.class, id);

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[Before:{}]", user.toJSON());
        }
        user.setUsername(username);
        user.setEnabled(enabled);
        if (password != null && !password.isEmpty()) {
            user.setPassword(password);

            List<AutoLogin> autoLogins = dao.findBy(AutoLogin.class, "user", user);
            autoLogins.forEach(autoLogin -> {
                if (LOGGER.isDebugEnabled()) {
                    debugRequest("[清理自动登入数据][autoLoginId={}]", autoLogin.getId());
                }
                dao.delete(autoLogin);
            });
        }

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[Modify:{}]", user.toJSON());
        }
        return objectResult(user.toJSON());
    }

}
