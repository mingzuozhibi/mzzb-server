package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscGroup;
import mingzuozhibi.persist.disc.DiscGroup.ViewType;
import mingzuozhibi.support.JsonArg;
import org.hibernate.Criteria;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import static org.hibernate.criterion.Restrictions.ne;

@RestController
public class DiscGroupController extends BaseController {

    @Transactional
    @GetMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "false") boolean hasPrivate) {
        Criteria criteria = dao.session().createCriteria(DiscGroup.class);
        if (!hasPrivate) {
            criteria.add(ne("viewType", ViewType.PrivateList));
        }

        @SuppressWarnings("unchecked")
        List<DiscGroup> discGroups = criteria.list();

        JSONArray array = discGroups.stream()
            .map(this::toJSON)
            .collect(toJSONArray());
        return objectResult(array);
    }

    private JSONObject toJSON(DiscGroup discGroup) {
        return discGroup.toJSON().put("discCount", discGroup.getDiscs().size());
    }

    private Collector<JSONObject, JSONArray, JSONArray> toJSONArray() {
        return Collector.of(JSONArray::new, JSONArray::put, (objects, objects2) -> {
            return objects.put(objects2.toList());
        });
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}", produces = MEDIA_TYPE)
    public String findOne(@PathVariable String key) {
        DiscGroup discGroup = dao.lookup(DiscGroup.class, "key", key);

        if (discGroup == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表失败][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        return objectResult(discGroup.toJSON());
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

        if (dao.lookup(DiscGroup.class, "key", key) != null) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[创建列表失败][该列表索引已存在][Key={}]", key);
            }
            return errorMessage("该列表索引已存在");
        }

        DiscGroup discGroup = new DiscGroup(key, title, enabled, viewType);
        dao.save(discGroup);

        JSONObject result = discGroup.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[创建列表成功][列表信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String setOne(
        @PathVariable("id") Long id,
        @JsonArg("$.key") String key,
        @JsonArg("$.title") String title,
        @JsonArg("$.enabled") boolean enabled,
        @JsonArg("$.viewType") ViewType viewType) {

        if (key.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][列表索引不能为空]");
            }
            return errorMessage("列表索引不能为空");
        }

        if (title.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][列表标题不能为空]");
            }
            return errorMessage("列表标题不能为空");
        }

        DiscGroup discGroup = dao.get(DiscGroup.class, id);

        if (discGroup == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][指定的列表Id不存在][id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        JSONObject before = discGroup.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑列表开始][修改前={}]", before);
        }

        discGroup.setKey(key);
        discGroup.setTitle(title);
        discGroup.setViewType(viewType);
        discGroup.setEnabled(enabled);

        JSONObject result = discGroup.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑列表成功][修改后={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String delOne(@PathVariable("id") Long id) {

        DiscGroup discGroup = dao.get(DiscGroup.class, id);
        if (dao.get(DiscGroup.class, id) == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[删除列表失败][指定的列表Id不存在][Id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        infoRequest("[删除列表开始][列表={}]", discGroup.getTitle());
        Set<Disc> discs = discGroup.getDiscs();
        if (LOGGER.isDebugEnabled()) {
            discs.forEach(disc -> {
                LOGGER.debug("[记录列表中的碟片][列表={}][碟片={}]", discGroup.getTitle(), disc.getLogName());
            });
        }

        int discCount = discs.size();
        discs.clear();
        dao.delete(discGroup);

        if (LOGGER.isDebugEnabled()) {
            infoRequest("[删除列表成功][该列表共有碟片{}个]", discCount);
        }
        return objectResult(discGroup.toJSON());
    }

}
