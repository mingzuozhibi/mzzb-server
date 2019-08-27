package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.persist.disc.Sakura.ViewType;
import mingzuozhibi.support.JsonArg;
import org.hibernate.Criteria;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collector;

import static org.hibernate.criterion.Restrictions.ne;

@RestController
public class DiscGroupController extends BaseController {

    @Deprecated
    @Transactional
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String findAllDeprecated(@RequestParam(name = "public", defaultValue = "true") boolean isPublic) {
        Criteria criteria = dao.session().createCriteria(Sakura.class);
        if (isPublic) {
            criteria.add(ne("viewType", ViewType.PrivateList));
        }

        @SuppressWarnings("unchecked")
        List<Sakura> discGroups = criteria.list();

        JSONArray array = discGroups.stream()
                .map(this::toJSONDeprecated)
                .collect(toJSONArray());
        return objectResult(array);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "false") boolean hasPrivate) {
        Criteria criteria = dao.session().createCriteria(Sakura.class);
        if (!hasPrivate) {
            criteria.add(ne("viewType", ViewType.PrivateList));
        }

        @SuppressWarnings("unchecked")
        List<Sakura> discGroups = criteria.list();

        JSONArray array = discGroups.stream()
                .map(this::toJSON)
                .collect(toJSONArray());
        return objectResult(array);
    }

    @Deprecated
    private JSONObject toJSONDeprecated(Sakura sakura) {
        return sakura.toJSON().put("discsSize", sakura.getDiscs().size());
    }

    private JSONObject toJSON(Sakura sakura) {
        return sakura.toJSON().put("discCount", sakura.getDiscs().size());
    }

    private Collector<JSONObject, JSONArray, JSONArray> toJSONArray() {
        return Collector.of(JSONArray::new, JSONArray::put, (objects, objects2) -> {
            return objects.put(objects2.toList());
        });
    }

    @Deprecated
    @Transactional
    @GetMapping(value = "/api/sakuras/key/{key}", produces = MEDIA_TYPE)
    public String findOneDeprecated(@PathVariable String key) {
        return findOne(key);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}", produces = MEDIA_TYPE)
    public String findOne(@PathVariable String key) {
        Sakura sakura = dao.lookup(Sakura.class, "key", key);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表失败][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        return objectResult(sakura.toJSON());
    }

    @Deprecated
    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String addOneDeprecated(
            @JsonArg String key,
            @JsonArg String title,
            @JsonArg(defaults = "true") boolean enabled,
            @JsonArg(defaults = "PublicList") ViewType viewType) {
        return addOne(key, title, enabled, viewType);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String addOne(
            @JsonArg String key,
            @JsonArg String title,
            @JsonArg(defaults = "true") boolean enabled,
            @JsonArg(defaults = "PublicList") ViewType viewType) {

        if (key.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[创建列表失败][列表索引不能为空]");
            }
            return errorMessage("列表索引不能为空");
        }

        if (title.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[创建列表失败][列表标题不能为空]");
            }
            return errorMessage("列表标题不能为空");
        }

        if (dao.lookup(Sakura.class, "key", key) != null) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[创建列表失败][该列表索引已存在][Key={}]", key);
            }
            return errorMessage("该列表索引已存在");
        }

        Sakura sakura = new Sakura(key, title, enabled, viewType);
        dao.save(sakura);

        JSONObject result = sakura.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[创建列表成功][列表信息={}]", result);
        }
        return objectResult(result);
    }

}
