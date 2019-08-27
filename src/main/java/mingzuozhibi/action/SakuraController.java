package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.persist.disc.Sakura.ViewType;
import mingzuozhibi.support.JsonArg;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class SakuraController extends BaseController {

    private final static String DISC_COLUMNS = "id,asin,thisRank,totalPt,title,titlePc,titleMo,surplusDays";

    private static Set<String> DISC_COLUMNS_SET = buildSet(DISC_COLUMNS);

    private static Set<String> buildSet(String columns) {
        return Stream.of(columns.split(",")).collect(Collectors.toSet());
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
