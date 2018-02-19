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
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String listSakura(@RequestParam("discColumns") String discColumns) {
        JSONArray data = new JSONArray();
        List<Sakura> sakuras = dao.findBy(Sakura.class, "enabled", true);

        Set<String> columns = Arrays.stream(discColumns.split(",")).collect(Collectors.toSet());
        sakuras.forEach(sakura -> {
            data.put(sakura.toJSON().put("discs", buildDiscs(sakura, columns)));
        });
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[size={}][discColumns={}]", sakuras.size(), discColumns);
        }
        return objectResult(data);
    }

    @Transactional
    @GetMapping(value = "/api/basic/sakuras", produces = MEDIA_TYPE)
    public String listBasicSakura() {
        JSONArray array = new JSONArray();
        dao.findAll(Sakura.class).forEach(sakura -> array.put(sakura.toJSON()));

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[size={}]", array.length());
        }
        return objectResult(array);
    }

    @Transactional
    @PostMapping(value = "/api/basic/sakuras", produces = MEDIA_TYPE)
    public String saveBasicSakura(
            @JsonArg("$.key") String key,
            @JsonArg("$.title") String title) {
        if (dao.lookup(Sakura.class, "key", key) != null) {
            return errorMessage("该Sakura列表已存在");
        }
        Sakura sakura = new Sakura(key, title);
        dao.save(sakura);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[json={}]", sakura.toJSON());
        }
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
