package mingzuozhibi.action;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Sakura;
import mingzuozhibi.persist.Sakura.ViewType;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.JsonArg;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class SakuraController extends BaseController {

    public static final String DISC_COLUMNS = "id,thisRank,prevRank,totalPt,title";

    @Autowired
    private Dao dao;

    @Transactional
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String listSakura(
            @RequestParam(name = "hasDiscs", defaultValue = "true") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns,
            @RequestParam(name = "viewType", defaultValue = "SakuraList") ViewType viewType) {
        JSONArray data = new JSONArray();

        @SuppressWarnings("unchecked")
        List<Sakura> sakuras = dao.query(session -> {
            return session.createCriteria(Sakura.class)
                    .add(Restrictions.eq("viewType", viewType))
                    .add(Restrictions.eq("enabled", true))
                    .list();
        });

        Set<String> columns = Arrays.stream(discColumns.split(",")).collect(Collectors.toSet());
        sakuras.forEach(sakura -> {
            JSONObject object = sakura.toJSON();
            if (hasDiscs) {
                object.put("discs", buildDiscs(sakura, columns));
            }
            data.put(object);
        });
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[size={}][discColumns={}]", sakuras.size(), discColumns);
        }
        return objectResult(data);
    }

    @Transactional
    @GetMapping(value = "/api/sakuras/{id}", produces = MEDIA_TYPE)
    public String viewSakura(
            @PathVariable("id") Long id,
            @RequestParam(name = "hasDiscs", defaultValue = "true") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {
        return responseViewSakura(dao.get(Sakura.class, id), hasDiscs, discColumns);
    }

    @Transactional
    @GetMapping(value = "/api/sakuras/key/{key}", produces = MEDIA_TYPE)
    public String viewSakuraByKey(
            @PathVariable("key") String key,
            @RequestParam(name = "hasDiscs", defaultValue = "true") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {
        return responseViewSakura(dao.lookup(Sakura.class, "key", key), hasDiscs, discColumns);
    }

    private String responseViewSakura(Sakura sakura, boolean hasDiscs, String discColumns) {
        if (sakura == null) {
            return errorMessage("指定的Sakura不存在");
        }

        JSONObject object = sakura.toJSON();

        if (hasDiscs) {
            Set<String> columns = Arrays.stream(discColumns.split(",")).collect(Collectors.toSet());
            object.put("discs", buildDiscs(sakura, columns));
        }

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[size={}][discColumns={}]", sakura.getDiscs().size(), discColumns);
        }
        return objectResult(object);
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
            @JsonArg("$.viewType") ViewType viewType) {
        if (dao.lookup(Sakura.class, "key", key) != null) {
            return errorMessage("该Sakura列表已存在");
        }
        Sakura sakura = new Sakura(key, title, viewType);
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
            @JsonArg("$.viewType") ViewType viewType,
            @JsonArg("$.enabled") boolean enabled) {
        Sakura sakura = dao.get(Sakura.class, id);

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[Before:{}]", sakura.toJSON());
        }
        sakura.setKey(key);
        sakura.setTitle(title);
        sakura.setViewType(viewType);
        sakura.setEnabled(enabled);
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[Modify:{}]", sakura.toJSON());
        }
        return objectResult(sakura.toJSON());
    }

}
