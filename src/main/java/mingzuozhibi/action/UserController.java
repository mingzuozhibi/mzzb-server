package mingzuozhibi.action;

import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController extends BaseController {

    @Autowired
    private Dao dao;

    @GetMapping(value = "/api/users", produces = CONTENT_TYPE)
    public String findAll() {
        JSONArray array = new JSONArray();
        dao.findAll(User.class).forEach(user -> {
            array.put(user.toJSON());
        });
        return objectResult(array);
    }

}
