package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.persist.disc.Sakura.ViewType;
import mingzuozhibi.support.JsonArg;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import static mingzuozhibi.action.DiscController.buildSet;

@RestController
public class SakuraController extends BaseController {

    public final static String DISC_COLUMNS = "id,asin,thisRank,totalPt,title,titlePc,titleMo,surplusDays";

    public static Set<String> DISC_COLUMNS_SET = buildSet(DISC_COLUMNS);

    @Transactional
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(name = "public", defaultValue = "true") boolean isPublic) {
        @SuppressWarnings("unchecked")
        List<Sakura> sakuras = dao.query(session -> {
            Criteria criteria = session.createCriteria(Sakura.class);
            if (isPublic) {
                criteria.add(Restrictions.ne("viewType", ViewType.PrivateList));
                criteria.add(Restrictions.eq("enabled", true));
            }
            return criteria.list();
        });

        JSONArray result = new JSONArray();

        sakuras.forEach(sakura -> {
            result.put(sakura.toJSON());
        });

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取多个列表成功][列表数量={}]", sakuras.size());
        }
        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/sakuras/key/{key}", produces = MEDIA_TYPE)
    public String findOne(@PathVariable String key) {
        Sakura sakura = dao.lookup(Sakura.class, "key", key);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表失败][指定的列表索引不存在][key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        JSONObject result = sakura.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取列表成功][列表信息={}]", sakura.toJSON());
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
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

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/sakuras/{id}", produces = MEDIA_TYPE)
    public String setOne(
            @PathVariable("id") Long id,
            @JsonArg("$.key") String key,
            @JsonArg("$.title") String title,
            @JsonArg("$.viewType") ViewType viewType,
            @JsonArg("$.enabled") boolean enabled) {

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

        Sakura sakura = dao.get(Sakura.class, id);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][指定的列表Id不存在][id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        JSONObject before = sakura.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑列表开始][修改前={}]", before);
        }

        sakura.setKey(key);
        sakura.setTitle(title);
        sakura.setViewType(viewType);
        sakura.setEnabled(enabled);

        JSONObject result = sakura.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑列表成功][修改后={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/api/sakuras/{id}", produces = MEDIA_TYPE)
    public String delOne(@PathVariable("id") Long id) {

        Sakura sakura = dao.get(Sakura.class, id);

        if (dao.get(Sakura.class, id) == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[删除列表失败][指定的列表Id不存在][Key={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        JSONObject before = sakura.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[删除列表开始][删除前={}]", before);
        }

        sakura.getDiscs().clear();

        dao.delete(sakura);

        JSONObject result = sakura.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[删除列表成功][修改后={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/sakuras/key/{key}/discs", produces = MEDIA_TYPE)
    public String findDiscs(
            @PathVariable String key,
            @RequestParam(defaultValue = DISC_COLUMNS) String discColumns,
            @RequestParam(name = "public", defaultValue = "true") boolean isPublic) {
        Sakura sakura = dao.lookup(Sakura.class, "key", key);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表碟片失败][指定的列表索引不存在][key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        JSONObject result = sakura.toJSON();

        JSONArray discs = new JSONArray();
        sakura.getDiscs().stream()
                .filter(disc -> !isPublic || disc.getUpdateType() != UpdateType.None)
                .forEach(disc -> discs.put(disc.toJSON(getColumns(discColumns))));
        result.put("discs", discs);

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取列表碟片成功][列表标题={}][碟片数量={}]",
                    sakura.getTitle(), sakura.getDiscs().size());
        }
        return objectResult(result);
    }

    private Set<String> getColumns(String discColumns) {
        switch (discColumns) {
            case DISC_COLUMNS:
                return DISC_COLUMNS_SET;
            default:
                return buildSet(discColumns);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/sakuras/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String pushDiscs(
            @PathVariable("id") Long id,
            @PathVariable("discId") Long discId,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {

        Sakura sakura = dao.get(Sakura.class, id);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加碟片到列表失败][指定的列表Id不存在][Id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        Disc disc = dao.get(Disc.class, discId);

        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加碟片到列表失败][指定的碟片Id不存在][Id={}]", discId);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        if (sakura.getDiscs().stream().anyMatch(d -> d.getId().equals(discId))) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[添加碟片到列表失败][指定的碟片已存在于列表][ASIN={}][列表={}]",
                        disc.getAsin(), sakura.getTitle());
            }
            return errorMessage("指定的碟片已存在于列表");
        }

        sakura.getDiscs().add(disc);

        dao.session().flush();

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[添加碟片到列表成功][ASIN={}][列表={}]", disc.getAsin(), sakura.getTitle());
        }

        return objectResult(disc.toJSON(getColumns(discColumns)));
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/sakuras/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String dropDiscs(
            @PathVariable("id") Long id,
            @PathVariable("discId") Long discId,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS) String discColumns) {

        Sakura sakura = dao.get(Sakura.class, id);

        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片失败][指定的列表Id不存在][Id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        Disc disc = sakura.getDiscs().stream()
                .filter(d -> d.getId().equals(discId))
                .findFirst().orElse(null);

        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片失败][指定的碟片不存在于列表][Id={}]", discId);
            }
            return errorMessage("指定的碟片不存在于列表");
        }

        sakura.getDiscs().remove(disc);

        dao.session().flush();

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[从列表移除碟片成功][ASIN={}][列表={}]", disc.getAsin(), sakura.getTitle());
        }
        return objectResult(disc.toJSON(getColumns(discColumns)));
    }

}
