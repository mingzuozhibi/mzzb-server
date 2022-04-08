package com.mingzuozhibi.action;

import com.mingzuozhibi.commons.BaseController;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscGroup;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class DiscGroupItemsController extends BaseController {

    @Autowired
    private JmsMessage jmsMessage;

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}/discs", produces = MEDIA_TYPE)
    public String findDiscs(
        @PathVariable String key,
        @RequestParam(required = false) String discColumns) {
        DiscGroup discGroup = dao.lookup(DiscGroup.class, "key", key);
        if (discGroup == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表碟片失败][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        JSONObject result = discGroup.toJSON();
        result.put("discs", buildDiscs(discGroup, discColumns));

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取列表碟片成功][列表标题={}][碟片数量={}]",
                discGroup.getTitle(), discGroup.getDiscs().size());
        }
        return objectResult(result);
    }

    private JSONArray buildDiscs(DiscGroup discGroup, String columns) {
        JSONArray discs = new JSONArray();
        if (columns == null) {
            discGroup.getDiscs().forEach(disc -> {
                discs.put(disc.toJSON());
            });
        } else {
            Set<String> columnSet = Stream.of(columns.split(","))
                .collect(Collectors.toSet());
            discGroup.getDiscs().forEach(disc -> {
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

        DiscGroup discGroup = dao.get(DiscGroup.class, id);
        if (discGroup == null) {
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

        if (discGroup.getDiscs().stream().anyMatch(d -> d.getId().equals(discId))) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[添加碟片到列表失败][指定的碟片已存在于列表][列表={}][碟片={}]",
                    discGroup.getTitle(), disc.getLogName());
            }
            return errorMessage("指定的碟片已存在于列表");
        }

        discGroup.getDiscs().add(disc);
        jmsMessage.info("[用户=%s][添加碟片成功][列表=%s][碟片=%s]", getUserName(), discGroup.getTitle(), disc.getLogName());
        return objectResult(disc.toJSON());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/discGroups/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String dropDiscs(
        @PathVariable("id") Long id,
        @PathVariable("discId") Long discId) {

        DiscGroup discGroup = dao.get(DiscGroup.class, id);
        if (discGroup == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片失败][指定的列表Id不存在][Id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        Disc disc = discGroup.getDiscs().stream()
            .filter(t -> t.getId().equals(discId))
            .findFirst().orElse(null);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[从列表移除碟片失败][指定的碟片Id不存在于列表][Id={}]", discId);
            }
            return errorMessage("指定的碟片Id不存在于列表");
        }

        discGroup.getDiscs().remove(disc);
        jmsMessage.info("[用户=%s][移除碟片成功][列表=%s][碟片=%s]", getUserName(), discGroup.getTitle(), disc.getLogName());
        return objectResult(disc.toJSON());
    }

}
