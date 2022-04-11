package com.mingzuozhibi.modules.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.commons.utils.ModifyUtils;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroup.ViewType;
import com.mingzuozhibi.support.JsonArg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.ChecksUtils.*;
import static com.mingzuozhibi.commons.utils.ModifyUtils.*;
import static com.mingzuozhibi.modules.group.DiscGroupUtils.buildWithCount;
import static com.mingzuozhibi.modules.group.DiscGroupUtils.buildWithDiscs;

@RestController
public class DiscGroupController extends BaseController2 {

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private DiscGroupRepository discGroupRepository;

    @Transactional
    @GetMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "false") boolean hasPrivate) {
        List<DiscGroup> discGroups = discGroupRepository.findAllHasPrivate(hasPrivate);
        JsonArray array = new JsonArray();
        discGroups.forEach(discGroup -> {
            long count = discRepository.countGroupDiscs(discGroup.getId());
            array.add(buildWithCount(discGroup, count));
        });
        return dataResult(array);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}", produces = MEDIA_TYPE)
    public String findByKey(@PathVariable String key) {
        Optional<DiscGroup> byKey = discGroupRepository.findByKey(key);
        if (!byKey.isPresent()) {
            return paramNotExists("列表索引");
        }
        return dataResult(byKey.get());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String doCreate(@JsonArg("$.key") String key,
                           @JsonArg("$.title") String title,
                           @JsonArg("$.enabled") Boolean enabled,
                           @JsonArg("$.viewType") ViewType viewType) {
        Optional<String> checks = runChecks(
            checkNotEmpty(key, "列表索引"),
            checkNotEmpty(title, "列表标题"),
            checkSelected(enabled, "是否更新"),
            checkSelected(viewType, "列表类型")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        if (discGroupRepository.findByKey(key).isPresent()) {
            return paramExists("列表索引");
        }
        DiscGroup discGroup = new DiscGroup(key, title, enabled, viewType);
        discGroupRepository.save(discGroup);
        jmsMessage.notify(logCreate("列表", discGroup.getTitle(), gson.toJson(discGroup)));
        return dataResult(discGroup);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String doUpdate(@PathVariable("id") Long id,
                           @JsonArg("$.key") String key,
                           @JsonArg("$.title") String title,
                           @JsonArg("$.enabled") Boolean enabled,
                           @JsonArg("$.viewType") ViewType viewType) {
        Optional<String> checks = runChecks(
            checkNotEmpty(key, "列表索引"),
            checkNotEmpty(title, "列表标题"),
            checkSelected(enabled, "是否更新"),
            checkSelected(viewType, "列表类型")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        Optional<DiscGroup> byId = discGroupRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byId.get();
        if (!Objects.equals(discGroup.getKey(), key)) {
            jmsMessage.info(logUpdate("列表索引", discGroup.getKey(), key));
            discGroup.setKey(key);
        }
        if (!Objects.equals(discGroup.getTitle(), title)) {
            jmsMessage.info(logUpdate("列表标题", discGroup.getTitle(), title));
            discGroup.setTitle(title);
        }
        if (!Objects.equals(discGroup.getViewType(), viewType)) {
            jmsMessage.info(logUpdate("列表类型", discGroup.getViewType(), viewType));
            discGroup.setViewType(viewType);
        }
        if (!Objects.equals(discGroup.isEnabled(), enabled)) {
            jmsMessage.info(logUpdate("是否更新", discGroup.isEnabled(), enabled));
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
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byId.get();
        if (discRepository.countGroupDiscs(id) > 0) {
            jmsMessage.warning(logDelete("列表", discGroup.getTitle(), gson.toJson(discGroup)));
            discGroup.getDiscs().forEach(disc -> {
                jmsMessage.info("[记录删除的碟片][ASIN=%s][NAME=%s]", disc.getAsin(), disc.getLogName());
            });
        } else {
            jmsMessage.notify(logDelete("列表", discGroup.getTitle(), gson.toJson(discGroup)));
        }
        discGroup.getDiscs().clear();
        discGroupRepository.delete(discGroup);
        return dataResult(discGroup);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}/discs", produces = MEDIA_TYPE)
    public String findDiscs(@PathVariable String key) {
        Optional<DiscGroup> byKey = discGroupRepository.findByKey(key);
        if (!byKey.isPresent()) {
            return paramNotExists("列表索引");
        }
        DiscGroup discGroup = byKey.get();
        JsonObject object = buildWithDiscs(discGroup);
        return dataResult(object);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String pushDiscs(@PathVariable Long id, @PathVariable Long discId) {
        Optional<DiscGroup> byId = discGroupRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byId.get();

        Optional<Disc> byId2 = discRepository.findById(discId);
        if (!byId2.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byId2.get();

        if (discRepository.existsDiscInGroup(discGroup, disc)) {
            return itemsExists("碟片");
        }
        discGroup.getDiscs().add(disc);

        jmsMessage.notify(ModifyUtils.logPush("碟片", disc.getLogName(), discGroup.getTitle()));
        return dataResult(disc.toJson());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/discGroups/{id}/discs/{discId}", produces = MEDIA_TYPE)
    public synchronized String dropDiscs(@PathVariable("id") Long id, @PathVariable("discId") Long discId) {
        Optional<DiscGroup> byId = discGroupRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byId.get();

        Optional<Disc> byId2 = discRepository.findById(discId);
        if (!byId2.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byId2.get();

        if (!discRepository.existsDiscInGroup(discGroup, disc)) {
            return itemsNotExists("碟片");
        }
        discGroup.getDiscs().remove(disc);

        jmsMessage.notify(logDrop("碟片", disc.getLogName(), discGroup.getTitle()));
        return dataResult(disc.toJson());
    }

}
