package mingzuozhibi.action;

import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.JsonArg;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController extends BaseController {

    @Autowired
    private Dao dao;

    @Transactional
    @GetMapping(value = "/api/admin/users", produces = MEDIA_TYPE)
    public String listAdminUser() {
        JSONArray array = new JSONArray();
        dao.findAll(User.class).forEach(user -> {
            array.put(user.toJSON());
        });

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}][{}][GET][/api/admin/users][size={}]",
                    getRemoteAddress(), getCurrentName(), array.length());
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

        JSONObject json = user.toJSON();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}][{}][POST][/api/admin/users][json={}]",
                    getRemoteAddress(), getCurrentName(), json);
        }
        return objectResult(json);
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
            LOGGER.debug("[{}][{}][POST][/api/admin/users/{}][Before:{}]",
                    getWebDetails().getRemoteAddress(), getCurrentName(), id, user.toJSON());
        }
        user.setUsername(username);
        user.setEnabled(enabled);
        if (password != null && !password.isEmpty()) {
            user.setPassword(password);
        }

        JSONObject json = user.toJSON();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}][{}][POST][/api/admin/users/{}][Modify:{}]",
                    getWebDetails().getRemoteAddress(), getCurrentName(), id, json);
        }
        return objectResult(json);
    }

}
