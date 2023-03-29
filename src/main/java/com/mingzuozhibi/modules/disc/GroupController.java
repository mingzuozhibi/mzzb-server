package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.Group.ViewType;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.Objects;

import static com.mingzuozhibi.commons.base.BaseController.DEFAULT_TYPE;
import static com.mingzuozhibi.commons.gson.GsonUtils.buildArray;
import static com.mingzuozhibi.modules.disc.DiscUtils.*;
import static com.mingzuozhibi.support.ChecksUtils.*;
import static com.mingzuozhibi.support.ModifyUtils.*;
import static java.util.Comparator.*;

@LoggerBind(Name.SERVER_USER)
@Transactional
@RestController
@RequestMapping(produces = DEFAULT_TYPE)
public class GroupController extends BaseController {

    public static final Comparator<Group> GROUP_COMPARATOR = comparing(Group::isEnabled, reverseOrder())
        .thenComparing(Group::getViewType, comparingInt(Enum::ordinal))
        .thenComparing(Group::getKey, reverseOrder());

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private GroupRepository groupRepository;

    @GetMapping("/api/discGroups/createMeta")
    public String createMeta() {
        var meta = new JsonObject();
        meta.add("keys", buildArray(groupRepository.listKeys()));
        meta.add("titles", buildArray(groupRepository.listTitles()));
        return dataResult(meta);
    }

    @GetMapping("/api/discGroups")
    public String findAll(@RequestParam(defaultValue = "top") String filter,
                          @RequestParam(defaultValue = "true") boolean withCount) {
        var array = new JsonArray();
        groupRepository.findByFilter(filter).stream()
            .sorted(GROUP_COMPARATOR)
            .forEach(group -> addGroupTo(array, group, withCount));
        return dataResult(array);
    }

    private void addGroupTo(JsonArray array, Group group, boolean withCount) {
        if (withCount) {
            array.add(buildWithCount(group, discRepository.countByGroup(group)));
        } else {
            array.add(gson.toJsonTree(group));
        }
    }

    @GetMapping("/api/discGroups/key/{key}")
    public String findByKey(@PathVariable String key) {
        var byKey = groupRepository.findByKey(key);
        if (byKey.isEmpty()) {
            return paramNotExists("列表索引");
        }
        return dataResult(byKey.get());
    }

    @GetMapping("/api/discGroups/asin/{asin}")
    public String findByAsin(@PathVariable String asin) {
        return dataResult(groupRepository.findByAsin(asin));
    }

    @Setter
    private static class EntityForm {
        private String key;
        private String title;
        private Boolean enabled;
        private ViewType viewType;
    }

    @PreAuthorize("hasRole('BASIC')")
    @PostMapping("/api/discGroups")
    public String doCreate(@RequestBody EntityForm form) {
        var checks = runChecks(
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
        var group = new Group(form.key, form.title, form.enabled, form.viewType);
        groupRepository.save(group);
        bind.success(logCreate("列表", group.getTitle(), gson.toJson(group)));
        return dataResult(group);
    }

    @PreAuthorize("hasRole('BASIC')")
    @PutMapping("/api/discGroups/{id}")
    public String doUpdate(@PathVariable("id") Long id,
                           @RequestBody EntityForm form) {
        var checks = runChecks(
            checkNotEmpty(form.key, "列表索引"),
            checkNotEmpty(form.title, "列表标题"),
            checkNotEmpty(form.enabled, "是否更新"),
            checkNotEmpty(form.viewType, "列表类型")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        var byId = groupRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("列表ID");
        }
        var group = byId.get();
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/discGroups/{id}")
    public String doDelete(@PathVariable("id") Long id) {
        var byId = groupRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("列表ID");
        }
        var group = byId.get();
        if (discRepository.countByGroup(group) > 0) {
            bind.warning(logDelete("列表", group.getTitle(), gson.toJson(group)));
            group.getDiscs().forEach(disc -> {
                bind.debug("[记录删除的碟片][ASIN=%s][NAME=%s]".formatted(disc.getAsin(), disc.getLogName()));
            });
        } else {
            bind.notify(logDelete("列表", group.getTitle(), gson.toJson(group)));
        }
        group.getDiscs().clear();
        groupRepository.delete(group);
        return dataResult(group);
    }

    @GetMapping("/api/discGroups/key/{key}/discs")
    public String findDiscs(@PathVariable String key) {
        var byKey = groupRepository.findByKey(key);
        if (byKey.isEmpty()) {
            return paramNotExists("列表索引");
        }
        var group = byKey.get();
        var object = buildWithDiscs(group);
        return dataResult(object);
    }

    @PreAuthorize("hasRole('BASIC')")
    @PostMapping("/api/discGroups/{gid}/discs/{did}")
    public synchronized String pushDiscs(@PathVariable Long gid,
                                         @PathVariable Long did) {
        var byGid = groupRepository.findById(gid);
        if (byGid.isEmpty()) {
            return paramNotExists("列表ID");
        }
        var group = byGid.get();

        var byDid = discRepository.findById(did);
        if (byDid.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byDid.get();

        if (discRepository.countByGroup(group, disc) > 0) {
            return itemsExists("碟片");
        }
        group.getDiscs().add(disc);

        bind.info(logPush("碟片", disc.getLogName(), group.getTitle()));
        return dataResult(disc.toJson());
    }

    @PreAuthorize("hasRole('BASIC')")
    @DeleteMapping("/api/discGroups/{gid}/discs/{did}")
    public synchronized String dropDiscs(@PathVariable("gid") Long gid,
                                         @PathVariable("did") Long did) {
        var byGid = groupRepository.findById(gid);
        if (byGid.isEmpty()) {
            return paramNotExists("列表ID");
        }
        var group = byGid.get();

        var byDid = discRepository.findById(did);
        if (byDid.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byDid.get();

        if (!(discRepository.countByGroup(group, disc) > 0)) {
            return itemsNotExists("碟片");
        }
        group.getDiscs().remove(disc);

        bind.info(logDrop("碟片", disc.getLogName(), group.getTitle()));
        return dataResult(disc.toJson());
    }

}
