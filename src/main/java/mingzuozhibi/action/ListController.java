package mingzuozhibi.action;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.DiscList;
import mingzuozhibi.persist.DiscList.ViewType;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mingzuozhibi.persist.DiscList.ViewType.PrivateList;

@RestController
public class ListController extends BaseController {

    private final static String DISC_COLUMNS = "id,thisRank,prevRank,totalPt,title";
    private final static String DISC_COLUMNS_ADMIN = "id,asin,thisRank,surplusDays,title";

    private static Set<String> DISC_COLUMNS_SET = buildSet(DISC_COLUMNS);
    private static Set<String> DISC_COLUMNS_ADMIN_SET = buildSet(DISC_COLUMNS_ADMIN);

    @Transactional
    @GetMapping(value = "/api/lists", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(name = "public", defaultValue = "true") boolean isPublic) {
        @SuppressWarnings("unchecked")
        List<DiscList> discLists = dao.query(session -> {
            Criteria criteria = session.createCriteria(DiscList.class);
            if (isPublic) {
                criteria.add(Restrictions.ne("viewType", PrivateList));
                criteria.add(Restrictions.eq("enabled", true));
            }
            return criteria.list();
        });

        JSONArray result = new JSONArray();

        discLists.forEach(discList -> {
            result.put(discList.toJSON());
        });

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[查询多个列表成功][列表数量={}]", discLists.size());
        }
        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/lists/key/{key}", produces = MEDIA_TYPE)
    public String findOne(@PathVariable String key) {
        DiscList discList = dao.lookup(DiscList.class, "key", key);

        if (discList == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[查询单个列表][指定的列表不存在][key={}]", key);
            }
            return errorMessage("指定的列表不存在");
        }

        JSONObject result = discList.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[查询单个列表成功][列表信息={}]", discList.toJSON());
        }
        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/lists/key/{key}/discs", produces = MEDIA_TYPE)
    public String findDiscs(
            @PathVariable String key,
            @RequestParam(defaultValue = DISC_COLUMNS) String discColumns) {
        DiscList discList = dao.lookup(DiscList.class, "key", key);

        if (discList == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[查询列表碟片][指定的列表不存在][key={}]", key);
            }
            return errorMessage("指定的列表不存在");
        }

        JSONObject result = discList.toJSON();

        JSONArray discs = new JSONArray();
        discList.getDiscs().stream()
                .filter(disc -> disc.getUpdateType() != Disc.UpdateType.None)
                .forEach(disc -> discs.put(disc.toJSON(getColumns(discColumns))));
        result.put("discs", discs);

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[查询列表碟片成功][列表标题={}][碟片数量={}]",
                    discList.getTitle(), discList.getDiscs().size());
        }
        return objectResult(result);
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
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/lists", produces = MEDIA_TYPE)
    public String addOne(
            @JsonArg String key,
            @JsonArg String title,
            @JsonArg(defaults = "true") boolean enabled,
            @JsonArg(defaults = "PublicList") ViewType viewType) {
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

        if (dao.lookup(DiscList.class, "key", key) != null) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[添加单个列表][指定的列表索引已存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引已存在");
        }

        DiscList discList = new DiscList(key, title, enabled, viewType);
        dao.save(discList);

        JSONObject result = discList.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[添加单个列表成功][列表信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/lists/{id}", produces = MEDIA_TYPE)
    public String setOne(
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

        DiscList discList = dao.get(DiscList.class, id);

        if (dao.lookup(DiscList.class, "key", key) == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑单个列表][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        JSONObject before = discList.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑单个列表][修改前={}]", before);
        }

        discList.setKey(key);
        discList.setTitle(title);
        discList.setViewType(viewType);
        discList.setEnabled(enabled);

        JSONObject result = discList.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑单个列表][修改后={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/lists/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public String pushDiscs(
            @PathVariable("id") Long id,
            @PathVariable("discId") Long discId,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS_ADMIN) String discColumns) {

        DiscList discList = dao.get(DiscList.class, id);

        if (discList == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加碟片到列表][指定的列表不存在][Id={}]", id);
            }
            return errorMessage("指定的列表不存在");
        }

        Disc disc = dao.get(Disc.class, discId);

        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加碟片到列表][指定的碟片不存在][Id={}]", discId);
            }
            return errorMessage("指定的碟片不存在");
        }

        if (discList.getDiscs().stream().anyMatch(d -> d.getId().equals(discId))) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[添加碟片到列表][指定的碟片已存在于列表][ASIN={}][列表={}]",
                        disc.getAsin(), discList.getTitle());
            }
            return errorMessage("指定的碟片已存在于列表");
        }

        discList.getDiscs().add(disc);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[添加碟片到列表成功][ASIN={}][列表={}]", disc.getAsin(), discList.getTitle());
        }
        return objectResult(disc.toJSON(getColumns(discColumns)));
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/lists/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public String dropDiscs(
            @PathVariable("id") Long id,
            @PathVariable("discId") Long discId,
            @RequestParam(name = "discColumns", defaultValue = DISC_COLUMNS_ADMIN) String discColumns) {

        DiscList discList = dao.get(DiscList.class, id);

        if (discList == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片][指定的列表不存在][Id={}]", id);
            }
            return errorMessage("指定的列表不存在");
        }

        Disc disc = discList.getDiscs().stream()
                .filter(d -> d.getId().equals(discId))
                .findFirst().orElse(null);

        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片][指定的碟片不存在于列表][Id={}]", discId);
            }
            return errorMessage("指定的碟片不存在于列表");
        }

        discList.getDiscs().remove(disc);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[从列表移除碟片成功][ASIN={}][列表={}]", disc.getAsin(), discList.getTitle());
        }
        return objectResult(disc.toJSON(getColumns(discColumns)));
    }

}
