package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.modules.disc.Group.ViewType;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.mingzuozhibi.modules.disc.DiscUtils.*;
import static com.mingzuozhibi.support.ChecksUtils.*;
import static com.mingzuozhibi.support.ModifyUtils.*;
import static java.util.Comparator.*;

@RestController
@LoggerBind(Name.SERVER_USER)
public class GroupController extends BaseController {

    public static final Comparator<Group> GROUP_COMPARATOR = comparing(Group::isEnabled, reverseOrder())
        .thenComparing(Group::getViewType, comparingInt(Enum::ordinal))
        .thenComparing(Group::getKey, reverseOrder());

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Transactional
    @GetMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "false") boolean hasPrivate,
                          @RequestParam(defaultValue = "true") boolean hasDisable) {
        List<Group> groups = groupRepository.findBy(hasPrivate, hasDisable);
        JsonArray array = new JsonArray();
        groups.stream().sorted(GROUP_COMPARATOR).forEach(group -> {
            long count = discRepository.countByGroup(group);
            array.add(buildWithCount(group, count));
        });
        return dataResult(array);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}", produces = MEDIA_TYPE)
    public String findByKey(@PathVariable String key) {
        Optional<Group> byKey = groupRepository.findByKey(key);
        if (byKey.isEmpty()) {
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
        if (groupRepository.findByKey(form.key).isPresent()) {
            return paramExists("列表索引");
        }
        Group group = new Group(form.key, form.title, form.enabled, form.viewType);
        groupRepository.save(group);
        bind.notify(logCreate("列表", group.getTitle(), gson.toJson(group)));
        return dataResult(group);
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
        Optional<Group> byId = groupRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("列表ID");
        }
        Group group = byId.get();
        if (!Objects.equals(group.getKey(), form.key)) {
            bind.notify(logUpdate("列表索引", group.getKey(), form.key, group.getTitle()));
            group.setKey(form.key);
        }
        if (!Objects.equals(group.getTitle(), form.title)) {
            bind.notify(logUpdate("列表标题", group.getTitle(), form.title, group.getTitle()));
            group.setTitle(form.title);
        }
        if (!Objects.equals(group.getViewType(), form.viewType)) {
            bind.notify(logUpdate("列表类型", group.getViewType(), form.viewType, group.getTitle()));
            group.setViewType(form.viewType);
        }
        if (!Objects.equals(group.isEnabled(), form.enabled)) {
            bind.notify(logUpdate("是否更新", group.isEnabled(), form.enabled, group.getTitle()));
            group.setEnabled(form.enabled);
        }
        return dataResult(group);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String doDelete(@PathVariable("id") Long id) {
        Optional<Group> byId = groupRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("列表ID");
        }
        Group group = byId.get();
        if (discRepository.countByGroup(group) > 0) {
            bind.warning(logDelete("列表", group.getTitle(), gson.toJson(group)));
            group.getDiscs().forEach(disc -> {
                String format = "[记录删除的碟片][ASIN=%s][NAME=%s]";
                bind.debug(String.format(format, disc.getAsin(), disc.getLogName()));
            });
        } else {
            bind.notify(logDelete("列表", group.getTitle(), gson.toJson(group)));
        }
        group.getDiscs().clear();
        groupRepository.delete(group);
        return dataResult(group);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}/discs", produces = MEDIA_TYPE)
    public String findDiscs(@PathVariable String key) {
        Optional<Group> byKey = groupRepository.findByKey(key);
        if (byKey.isEmpty()) {
            return paramNotExists("列表索引");
        }
        Group group = byKey.get();
        JsonObject object = buildWithDiscs(group);
        return dataResult(object);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups/{gid}/discs/{did}", produces = MEDIA_TYPE)
    public synchronized String pushDiscs(@PathVariable Long gid,
                                         @PathVariable Long did) {
        Optional<Group> byGid = groupRepository.findById(gid);
        if (byGid.isEmpty()) {
            return paramNotExists("列表ID");
        }
        Group group = byGid.get();

        Optional<Disc> byDid = discRepository.findById(did);
        if (byDid.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byDid.get();

        if (discRepository.countByGroup(group, disc) > 0) {
            return itemsExists("碟片");
        }
        group.getDiscs().add(disc);

        bind.info(logPush("碟片", disc.getLogName(), group.getTitle()));
        return dataResult(disc.toJson());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping(value = "/api/discGroups/{gid}/discs/{did}", produces = MEDIA_TYPE)
    public synchronized String dropDiscs(@PathVariable("gid") Long gid,
                                         @PathVariable("did") Long did) {
        Optional<Group> byGid = groupRepository.findById(gid);
        if (byGid.isEmpty()) {
            return paramNotExists("列表ID");
        }
        Group group = byGid.get();

        Optional<Disc> byDid = discRepository.findById(did);
        if (byDid.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byDid.get();

        if (!(discRepository.countByGroup(group, disc) > 0)) {
            return itemsNotExists("碟片");
        }
        group.getDiscs().remove(disc);

        bind.info(logDrop("碟片", disc.getLogName(), group.getTitle()));
        return dataResult(disc.toJson());
    }

}
