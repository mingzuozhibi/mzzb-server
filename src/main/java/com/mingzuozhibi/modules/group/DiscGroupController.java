package com.mingzuozhibi.modules.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroup.ViewType;
import com.mingzuozhibi.utils.ModifyUtils;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;
import static com.mingzuozhibi.modules.group.DiscGroupUtils.*;
import static com.mingzuozhibi.utils.ChecksUtils.*;
import static com.mingzuozhibi.utils.ModifyUtils.*;

@RestController
public class DiscGroupController extends BaseController {

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

    @Setter
    private static class EntityForm {
        private String key;
        private String title;
        private Boolean enabled;
        private ViewType viewType;
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String doCreate(@RequestBody EntityForm form) {
        Optional<String> checks = runChecks(
            checkNotEmpty(form.key, "列表索引"),
            checkNotEmpty(form.title, "列表标题"),
            checkNotEmpty(form.enabled, "是否更新"),
            checkNotEmpty(form.viewType, "列表类型")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        if (discGroupRepository.findByKey(form.key).isPresent()) {
            return paramExists("列表索引");
        }
        DiscGroup discGroup = new DiscGroup(form.key, form.title, form.enabled, form.viewType);
        discGroupRepository.save(discGroup);
        jmsMessage.notify(logCreate("列表", discGroup.getTitle(), GSON.toJson(discGroup)));
        return dataResult(discGroup);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String doUpdate(@PathVariable("id") Long id,
                           @RequestBody EntityForm form) {
        Optional<String> checks = runChecks(
            checkNotEmpty(form.key, "列表索引"),
            checkNotEmpty(form.title, "列表标题"),
            checkNotEmpty(form.enabled, "是否更新"),
            checkNotEmpty(form.viewType, "列表类型")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        Optional<DiscGroup> byId = discGroupRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byId.get();
        if (!Objects.equals(discGroup.getKey(), form.key)) {
            jmsMessage.info(logUpdate("列表索引", discGroup.getKey(), form.key));
            discGroup.setKey(form.key);
        }
        if (!Objects.equals(discGroup.getTitle(), form.title)) {
            jmsMessage.info(logUpdate("列表标题", discGroup.getTitle(), form.title));
            discGroup.setTitle(form.title);
        }
        if (!Objects.equals(discGroup.getViewType(), form.viewType)) {
            jmsMessage.info(logUpdate("列表类型", discGroup.getViewType(), form.viewType));
            discGroup.setViewType(form.viewType);
        }
        if (!Objects.equals(discGroup.isEnabled(), form.enabled)) {
            jmsMessage.info(logUpdate("是否更新", discGroup.isEnabled(), form.enabled));
            discGroup.setEnabled(form.enabled);
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
            jmsMessage.warning(logDelete("列表", discGroup.getTitle(), GSON.toJson(discGroup)));
            discGroup.getDiscs().forEach(disc -> {
                jmsMessage.info("[记录删除的碟片][ASIN=%s][NAME=%s]", disc.getAsin(), disc.getLogName());
            });
        } else {
            jmsMessage.notify(logDelete("列表", discGroup.getTitle(), GSON.toJson(discGroup)));
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
    @PostMapping(value = "/api/discGroups/{gid}/discs/{did}", produces = MEDIA_TYPE)
    public synchronized String pushDiscs(@PathVariable Long gid,
                                         @PathVariable Long did) {
        Optional<DiscGroup> byGid = discGroupRepository.findById(gid);
        if (!byGid.isPresent()) {
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byGid.get();

        Optional<Disc> byDid = discRepository.findById(did);
        if (!byDid.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byDid.get();

        if (discRepository.existsDiscInGroup(discGroup, disc)) {
            return itemsExists("碟片");
        }
        discGroup.getDiscs().add(disc);

        jmsMessage.notify(ModifyUtils.logPush("碟片", disc.getLogName(), discGroup.getTitle()));
        return dataResult(disc.toJson());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/discGroups/{gid}/discs/{did}", produces = MEDIA_TYPE)
    public synchronized String dropDiscs(@PathVariable("gid") Long gid,
                                         @PathVariable("did") Long did) {
        Optional<DiscGroup> byGid = discGroupRepository.findById(gid);
        if (!byGid.isPresent()) {
            return paramNotExists("列表ID");
        }
        DiscGroup discGroup = byGid.get();

        Optional<Disc> byDid = discRepository.findById(did);
        if (!byDid.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byDid.get();

        if (!discRepository.existsDiscInGroup(discGroup, disc)) {
            return itemsNotExists("碟片");
        }
        discGroup.getDiscs().remove(disc);

        jmsMessage.notify(logDrop("碟片", disc.getLogName(), discGroup.getTitle()));
        return dataResult(disc.toJson());
    }

}
