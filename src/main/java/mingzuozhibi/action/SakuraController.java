package mingzuozhibi.action;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Sakura;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.JsonArg;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static mingzuozhibi.persist.Sakura.ViewType.SakuraList;

@RestController
public class SakuraController extends BaseController {

    @Autowired
    private Dao dao;

    @Transactional
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String listSakura(@RequestParam("discColumns") String discColumns) {
        JSONArray data = new JSONArray();

        @SuppressWarnings("unchecked")
        List<Sakura> sakuras = dao.query(session -> {
            return session.createCriteria(Sakura.class)
                    .add(Restrictions.eq("viewType", SakuraList))
                    .add(Restrictions.eq("enabled", true))
                    .list();
        });

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
            @JsonArg("$.title") String title,
            @JsonArg("$.viewType") String viewType) {
        if (dao.lookup(Sakura.class, "key", key) != null) {
            return errorMessage("该Sakura列表已存在");
        }
        Sakura sakura = new Sakura(key, title, Sakura.ViewType.valueOf(viewType));
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

    @Transactional
    @PostMapping(value = "/api/basic/sakuras/{id}", produces = MEDIA_TYPE)
    public String editAdminUser(
            @PathVariable("id") Long id,
            @JsonArg("$.key") String key,
            @JsonArg("$.title") String title,
            @JsonArg("$.viewType") String viewType,
            @JsonArg("$.enabled") boolean enabled) {
        Sakura sakura = dao.get(Sakura.class, id);

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[Before:{}]", sakura.toJSON());
        }
        sakura.setKey(key);
        sakura.setTitle(title);
        sakura.setViewType(Sakura.ViewType.valueOf(viewType));
        sakura.setEnabled(enabled);
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[Modify:{}]", sakura.toJSON());
        }
        return objectResult(sakura.toJSON());
    }

}
