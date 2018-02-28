package mingzuozhibi.action;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Sakura;
import mingzuozhibi.persist.Sakura.ViewType;
import mingzuozhibi.support.JsonArg;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class SakuraController extends BaseController {

    private final static String DISC_COLUMNS = "id,thisRank,prevRank,totalPt,title";
    private final static String DISC_COLUMNS_ADMIN = "id,asin,thisRank,surplusDays,title";
    private final static String FIND_ONE_KEYS = "key";

    private static Set<String> DISC_COLUMNS_SET = buildSet(DISC_COLUMNS);
    private static Set<String> DISC_COLUMNS_ADMIN_SET = buildSet(DISC_COLUMNS_ADMIN);
    private static Set<String> FIND_ONE_KEYS_SET = buildSet(FIND_ONE_KEYS);

    @Transactional
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String findAll(
            @RequestParam(name = "viewType", defaultValue = "SakuraList") ViewType viewType,
            @RequestParam(name = "hasDiscs", defaultValue = "true") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {

        @SuppressWarnings("unchecked")
        List<Sakura> sakuras = dao.query(session -> {
            return session.createCriteria(Sakura.class)
                    .add(Restrictions.eq("viewType", viewType))
                    .add(Restrictions.eq("enabled", true))
                    .list();
        });

        return responseAll(sakuras, hasDiscs, discColumns);
    }

    private String responseAll(List<Sakura> sakuras, boolean hasDiscs, String discColumns) {
        JSONArray result = new JSONArray();

        AtomicInteger discConut = new AtomicInteger();
        if (hasDiscs) {
            Set<String> columns = getColumns(discColumns);
            sakuras.forEach(sakura -> {
                discConut.addAndGet(sakura.getDiscs().size());
                result.put(sakura.toJSON().put("discs", buildDiscs(sakura, columns)));
            });
        } else {
            sakuras.forEach(sakura -> {
                result.put(sakura.toJSON());
            });
        }

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[查询多个列表成功][列表数量={}][碟片数量={}]", sakuras.size(), discConut.get());
        }
        return objectResult(result);
    }

    private JSONArray buildDiscs(Sakura sakura, Set<String> columns) {
        JSONArray discs = new JSONArray();
        sakura.getDiscs().stream()
                .filter(disc -> disc.getUpdateType() != Disc.UpdateType.None)
                .forEach(disc -> discs.put(disc.toJSON(columns)));
        return discs;
    }

    private Set<String> getColumns(String discColumns) {
        switch (discColumns) {
            case DISC_COLUMNS:
                return DISC_COLUMNS_SET;
            case DISC_COLUMNS_ADMIN:
                return DISC_COLUMNS_ADMIN_SET;
            default:
                return buildSet(discColumns);
        }
    }

    private static Set<String> buildSet(String discColumns) {
        return Stream.of(discColumns.split(",")).collect(Collectors.toSet());
    }

    @Transactional
    @GetMapping(value = "/api/sakuras/{key}/{value}", produces = MEDIA_TYPE)
    public String findOne(
            @PathVariable("key") String key,
            @PathVariable("value") String value,
            @RequestParam(name = "hasDiscs", defaultValue = "true") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {

        if (!FIND_ONE_KEYS_SET.contains(key)) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[查询单个列表][不支持的查询属性][key={}]", key);
            }
            return errorMessage("不支持的查询属性");
        }

        Sakura sakura = dao.lookup(Sakura.class, key, value);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[查询单个列表][指定的列表不存在][key={}][value={}]", key, value);
            }
            return errorMessage("指定的列表不存在");
        }

        return responseOne(sakura, hasDiscs, discColumns);
    }

    private String responseOne(Sakura sakura, boolean hasDiscs, String discColumns) {
        JSONObject result = sakura.toJSON();
        if (hasDiscs) {
            result.put("discs", buildDiscs(sakura, getColumns(discColumns)));
        }

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[查询单个列表成功][列表信息={}][碟片数量={}]",
                    sakura.toJSON(), hasDiscs ? sakura.getDiscs().size() : 0);
        }
        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/sakuras/{id}", produces = MEDIA_TYPE)
    public String getOne(
            @PathVariable("id") Long id,
            @RequestParam(name = "hasDiscs", defaultValue = "true") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {

        Sakura sakura = dao.get(Sakura.class, id);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[查询单个列表][指定的列表不存在][Id={}]", id);
            }
            return errorMessage("指定的列表不存在");
        }

        return responseOne(sakura, hasDiscs, discColumns);
    }

    @Transactional
    @GetMapping(value = "/api/basic/sakuras", produces = MEDIA_TYPE)
    public String adminFindAll() {

        return responseAll(dao.findAll(Sakura.class), false, null);
    }

    @Transactional
    @GetMapping(value = "/api/basic/sakuras/{key}/{value}", produces = MEDIA_TYPE)
    public String adminFindOne(
            @PathVariable("key") String key,
            @PathVariable("value") String value,
            @RequestParam(name = "hasDiscs", defaultValue = "false") boolean hasDiscs,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS_ADMIN) String discColumns) {

        return findOne(key, value, hasDiscs, discColumns);
    }

    @Transactional
    @PostMapping(value = "/api/basic/sakuras", produces = MEDIA_TYPE)
    public String adminAddOne(
            @JsonArg("$.key") String key,
            @JsonArg("$.title") String title,
            @JsonArg("$.viewType") ViewType viewType) {
        if (key.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加单个列表][列表索引不能为空]");
            }
            return errorMessage("列表索引不能为空");
        }

        if (title.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加单个列表][列表标题不能为空]");
            }
            return errorMessage("列表标题不能为空");
        }

        if (dao.lookup(Sakura.class, "key", key) != null) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[添加单个列表][指定的列表索引已存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引已存在");
        }

        Sakura sakura = new Sakura(key, title, viewType);
        dao.save(sakura);

        JSONObject result = sakura.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[添加单个列表成功][列表信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PostMapping(value = "/api/basic/sakuras/{id}", produces = MEDIA_TYPE)
    public String adminSetOne(
            @PathVariable("id") Long id,
            @JsonArg("$.key") String key,
            @JsonArg("$.title") String title,
            @JsonArg("$.viewType") ViewType viewType,
            @JsonArg("$.enabled") boolean enabled) {

        if (key.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑单个列表][列表索引不能为空]");
            }
            return errorMessage("列表索引不能为空");
        }

        if (title.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑单个列表][列表标题不能为空]");
            }
            return errorMessage("列表标题不能为空");
        }

        Sakura sakura = dao.get(Sakura.class, id);

        if (dao.lookup(Sakura.class, "key", key) == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑单个列表][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        JSONObject before = sakura.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑单个列表][修改前={}]", before);
        }

        sakura.setKey(key);
        sakura.setTitle(title);
        sakura.setViewType(viewType);
        sakura.setEnabled(enabled);

        JSONObject result = sakura.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑单个列表][修改后={}]", result);
        }
        return objectResult(result);
    }

}
