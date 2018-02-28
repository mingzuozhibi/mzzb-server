package mingzuozhibi.action;

import mingzuozhibi.persist.AutoLogin;
import mingzuozhibi.persist.User;
import mingzuozhibi.support.JsonArg;
import org.json.JSONArray;
import org.json.JSONObject;
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
    public String findAll() {
        JSONArray result = new JSONArray();
        dao.findAll(User.class).forEach(user -> {
            result.put(user.toJSON());
        });

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取用户列表成功][用户数量={}]", result.length());
        }
        return objectResult(result);
    }

    @Transactional
    @PostMapping(value = "/api/admin/users", produces = MEDIA_TYPE)
    public String addOne(
            @JsonArg("$.username") String username,
            @JsonArg("$.password") String password) {
        if (dao.lookup(User.class, "username", username) != null) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[添加单个用户][该用户名已存在][用户名={}]", username);
            }
            return errorMessage("该用户名已存在");
        }
        User user = new User(username, password);
        dao.save(user);

        JSONObject result = user.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[添加单个用户成功][用户信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/admin/users/{id}", produces = MEDIA_TYPE)
    public String getOne(@PathVariable("id") Long id) {
        User user = dao.get(User.class, id);
        if (user == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取单个用户][指定的用户不存在][Id={}]", id);
            }
            return errorMessage("指定的用户不存在");
        }

        JSONObject result = user.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取单个用户成功][用户信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PostMapping(value = "/api/admin/users/{id}", produces = MEDIA_TYPE)
    public String setOne(
            @PathVariable("id") Long id,
            @JsonArg("$.username") String username,
            @JsonArg("$.password") String password,
            @JsonArg("$.enabled") boolean enabled) {

        User user = dao.get(User.class, id);

        if (user == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取单个用户][指定的用户不存在][Id={}]", id);
            }
            return errorMessage("指定的用户不存在");
        }

        if (LOGGER.isDebugEnabled()) {
            JSONObject before = user.toJSON();
            debugRequest("[编辑单个用户][修改前={}]", before);
        }

        user.setUsername(username);
        user.setEnabled(enabled);
        if (password != null && !password.isEmpty()) {
            user.setPassword(password);
            cleanAutoLogin(user);
        }

        JSONObject result = user.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑单个用户][修改后={}]", result);
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
