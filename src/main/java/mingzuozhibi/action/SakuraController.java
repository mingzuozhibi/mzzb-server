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

}
