package mingzuozhibi.action;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Sakura;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.JsonArg;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class SakuraController extends BaseController {

    @Autowired
    private Dao dao;

    @Transactional
    @GetMapping(value = "/api/sakuras", produces = CONTENT_TYPE)
    public String listSakura(@RequestParam("discColumns") String discColumns) {
        JSONArray data = new JSONArray();
        List<Sakura> sakuras = dao.findBy(Sakura.class, "enabled", true);
        LOGGER.debug("获取sakura数据, 共{}个列表, 请求参数: {}", sakuras.size(), discColumns);
        Set<String> columns = Arrays.stream(discColumns.split(",")).collect(Collectors.toSet());
        sakuras.forEach(sakura -> {
            data.put(sakura.toJSON().put("discs", buildDiscs(sakura, columns)));
            LOGGER.debug("列表[{}]共{}个碟片", sakura.getTitle(), sakura.getDiscs().size());
        });
        return objectResult(data);
    }

    @Transactional
    @GetMapping(value = "/api/basic/sakuras", produces = CONTENT_TYPE)
    public String listBasicSakura() {
        JSONArray data = new JSONArray();
        List<Sakura> sakuras = dao.findAll(Sakura.class);
        sakuras.forEach(sakura -> data.put(sakura.toJSON()));
        return objectResult(data);
    }

    @Transactional
    @PostMapping(value = "/api/basic/sakuras", produces = CONTENT_TYPE)
    public String saveBasicSakura(
            @JsonArg("$.key") String key,
            @JsonArg("$.title") String title) {
        if (dao.lookup(Sakura.class, "key", key) != null) {
            return errorMessage("该Sakura列表已存在");
        }
        Sakura sakura = new Sakura(key, title);
        dao.save(sakura);
        return objectResult(sakura.toJSON());
    }

    private JSONArray buildDiscs(Sakura sakura, Set<String> columns) {
        JSONArray discs = new JSONArray();
        sakura.getDiscs().stream()
                .filter(disc -> disc.getUpdateType() != Disc.UpdateType.None)
                .forEach(disc -> discs.put(disc.toJSON(columns)));
        return discs;
    }

}
