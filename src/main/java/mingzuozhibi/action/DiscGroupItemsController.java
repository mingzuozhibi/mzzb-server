package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Sakura;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class DiscGroupItemsController extends BaseController {

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}/discs", produces = MEDIA_TYPE)
    public String findDiscs(
            @PathVariable String key,
            @RequestParam(required = false) String discColumns) {
        Sakura sakura = dao.lookup(Sakura.class, "key", key);
        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表碟片失败][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        JSONObject result = sakura.toJSON();
        result.put("discs", buildDiscs(sakura, discColumns));

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取列表碟片成功][列表标题={}][碟片数量={}]",
                    sakura.getTitle(), sakura.getDiscs().size());
        }
        return objectResult(result);
    }

    private JSONArray buildDiscs(Sakura sakura, String columns) {
        JSONArray discs = new JSONArray();
        if (columns == null) {
            sakura.getDiscs().forEach(disc -> {
                discs.put(disc.toJSON());
            });
        } else {
            Set<String> columnSet = Stream.of(columns.split(","))
                    .collect(Collectors.toSet());
            sakura.getDiscs().forEach(disc -> {
                discs.put(disc.toJSON(columnSet));
            });
        }
        return discs;
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String pushDiscs(
            @PathVariable Long id,
            @PathVariable Long discId) {

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
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加碟片到列表失败][指定的碟片已存在于列表][列表={}][碟片={}]",
                        sakura.getTitle(), disc.getLogName());
            }
            return errorMessage("指定的碟片已存在于列表");
        }

        sakura.getDiscs().add(disc);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[添加碟片到列表成功][列表={}][碟片={}]", sakura.getTitle(), disc.getLogName());
        }
        return objectResult(disc.toJSON());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/discGroups/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String dropDiscs(
            @PathVariable("id") Long id,
            @PathVariable("discId") Long discId) {

        Sakura sakura = dao.get(Sakura.class, id);
        if (sakura == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片失败][指定的列表Id不存在][Id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        Disc disc = sakura.getDiscs().stream()
                .filter(t -> t.getId().equals(discId))
                .findFirst().orElse(null);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片失败][指定的碟片Id不存在于列表][Id={}]", discId);
            }
            return errorMessage("指定的碟片Id不存在于列表");
        }

        sakura.getDiscs().remove(disc);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[从列表移除碟片成功][列表={}][碟片={}]", sakura.getTitle(), disc.getLogName());
        }
        return objectResult(disc.toJSON());
    }

}
