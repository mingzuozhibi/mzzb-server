package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.BaseController2;
import com.mingzuozhibi.commons.check.CheckResult;
import com.mingzuozhibi.commons.check.CheckUtils;
import com.mingzuozhibi.commons.gson.InstantUtils;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.modules.disc.DiscGroup.ViewType;
import com.mingzuozhibi.support.JsonArg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.check.CheckHelper.*;
import static com.mingzuozhibi.commons.check.CheckUtils.paramNoExists;
import static java.util.Comparator.*;

@RestController
public class DiscGroupController extends BaseController2 {

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private DiscGroupRepository discGroupRepository;

    @Transactional
    @GetMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "false") boolean hasPrivate) {
        List<DiscGroup> discGroups;
        if (hasPrivate) {
            discGroups = discGroupRepository.findAll();
        } else {
            discGroups = discGroupRepository.findByViewTypeNot(ViewType.PrivateList);
        }
        JsonArray array = new JsonArray();
        discGroups.forEach(discGroup -> {
            JsonObject object = gson.toJsonTree(discGroup).getAsJsonObject();
            object.addProperty("discCount", discGroupRepository.countDiscsById(discGroup.getId()));
            array.add(object);
        });
        return dataResult(array);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}", produces = MEDIA_TYPE)
    public String findByKey(@PathVariable String key) {
        Optional<DiscGroup> byKey = discGroupRepository.findByKey(key);
        if (!byKey.isPresent()) {
            return paramNoExists("列表索引");
        }
        return dataResult(byKey.get());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String doUpdate(@PathVariable("id") Long id,
                           @JsonArg("$.key") String key,
                           @JsonArg("$.title") String title,
                           @JsonArg("$.enabled") Boolean enabled,
                           @JsonArg("$.viewType") ViewType viewType) {
        CheckResult checks = runAllCheck(
            checkNotEmpty(key, "列表索引"),
            checkNotEmpty(title, "列表标题"),
            checkSelected(enabled, "是否更新"),
            checkSelected(viewType, "列表类型")
        );
        if (checks.hasError()) {
            return errorResult(checks.getError());
        }
        Optional<DiscGroup> byId = discGroupRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNoExists("列表ID");
        }
        DiscGroup discGroup = byId.get();
        if (!Objects.equals(discGroup.getKey(), key)) {
            jmsMessage.info(CheckUtils.doUpdate("列表索引", discGroup.getKey(), key));
            discGroup.setKey(key);
        }
        if (!Objects.equals(discGroup.getTitle(), title)) {
            jmsMessage.info(CheckUtils.doUpdate("列表标题", discGroup.getTitle(), title));
            discGroup.setTitle(title);
        }
        if (!Objects.equals(discGroup.getViewType(), viewType)) {
            jmsMessage.info(CheckUtils.doUpdate("列表类型", discGroup.getViewType(), viewType));
            discGroup.setViewType(viewType);
        }
        if (!Objects.equals(discGroup.isEnabled(), enabled)) {
            jmsMessage.info(CheckUtils.doUpdate("是否更新", discGroup.isEnabled(), enabled));
            discGroup.setEnabled(enabled);
        }
        return dataResult(discGroup);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String doDelete(@PathVariable("id") Long id) {
        Optional<DiscGroup> byId = discGroupRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNoExists("列表ID");
        }
        DiscGroup discGroup = byId.get();
        if (discGroupRepository.countDiscsById(id) > 0) {
            jmsMessage.warning(CheckUtils.doDelete("列表", discGroup.getTitle(), gson.toJson(discGroup)));
            discGroup.getDiscs().forEach(disc -> {
                jmsMessage.info("[记录删除的碟片][ASIN=%s][NAME=%s]", disc.getAsin(), disc.getLogName());
            });
        } else {
            jmsMessage.notify(CheckUtils.doDelete("列表", discGroup.getTitle(), gson.toJson(discGroup)));
        }
        discGroup.getDiscs().clear();
        discGroupRepository.delete(discGroup);
        return dataResult(discGroup);
    }

}
