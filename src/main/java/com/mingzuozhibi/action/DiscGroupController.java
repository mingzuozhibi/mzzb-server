package com.mingzuozhibi.action;

import com.mingzuozhibi.commons.BaseController;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.modules.disc.DiscGroup;
import com.mingzuozhibi.modules.disc.DiscGroup.ViewType;
import com.mingzuozhibi.persist.disc.Disc;
import com.mingzuozhibi.support.JsonArg;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Set;

@RestController
public class DiscGroupController extends BaseController {

    @Autowired
    private JmsMessage jmsMessage;

    @Transactional
    @GetMapping(value = "/api/discGroups/key/{key}", produces = MEDIA_TYPE)
    public String findOne(@PathVariable String key) {
        DiscGroup discGroup = dao.lookup(DiscGroup.class, "key", key);

        if (discGroup == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取列表失败][指定的列表索引不存在][Key={}]", key);
            }
            return errorMessage("指定的列表索引不存在");
        }

        return objectResult(discGroup.toJSON());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String addOne(@JsonArg String key, @JsonArg String title, @JsonArg(defaults = "true") boolean enabled,
                         @JsonArg(defaults = "PublicList") ViewType viewType) {

        if (key.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[创建列表失败][列表索引不能为空]");
            }
            return errorMessage("列表索引不能为空");
        }

        if (title.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[创建列表失败][列表标题不能为空]");
            }
            return errorMessage("列表标题不能为空");
        }

        if (dao.lookup(DiscGroup.class, "key", key) != null) {
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[创建列表失败][该列表索引已存在][Key={}]", key);
            }
            return errorMessage("该列表索引已存在");
        }

        DiscGroup discGroup = new DiscGroup(key, title, enabled, viewType);
        dao.save(discGroup);

        JSONObject result = discGroup.toJSON();
        jmsMessage.success("[用户=%s][创建列表成功][列表=%s]", getUserName(), result.toString());
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String setOne(@PathVariable("id") Long id, @JsonArg("$.key") String key, @JsonArg("$.title") String title,
                         @JsonArg("$.enabled") boolean enabled, @JsonArg("$.viewType") ViewType viewType) {

        if (key.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][列表索引不能为空]");
            }
            return errorMessage("列表索引不能为空");
        }

        if (title.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][列表标题不能为空]");
            }
            return errorMessage("列表标题不能为空");
        }

        DiscGroup discGroup = dao.get(DiscGroup.class, id);

        if (discGroup == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][指定的列表Id不存在][id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        JSONObject before = discGroup.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑列表开始][修改前={}]", before);
        }

        if (!Objects.equals(discGroup.getKey(), key)) {
            jmsMessage.info("[用户=%s][修改列表索引][%s=>%s]", getUserName(), discGroup.getKey(), key);
            discGroup.setKey(key);
        }
        if (!Objects.equals(discGroup.getTitle(), title)) {
            jmsMessage.info("[用户=%s][修改列表标题][%s=>%s]", getUserName(), discGroup.getTitle(), title);
            discGroup.setTitle(title);
        }
        if (!Objects.equals(discGroup.getViewType(), viewType)) {
            jmsMessage.info("[用户=%s][修改列表显示类型][%s=>%s]", getUserName(), discGroup.getViewType().name(),
                viewType.name());
            discGroup.setViewType(viewType);
        }
        if (discGroup.isEnabled() != enabled) {
            jmsMessage.info("[用户=%s][修改列表启用状态][%b=>%b]", getUserName(), discGroup.isEnabled(), enabled);
            discGroup.setEnabled(enabled);
        }

        JSONObject result = discGroup.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑列表成功][修改后={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/api/discGroups/{id}", produces = MEDIA_TYPE)
    public String delOne(@PathVariable("id") Long id) {

        DiscGroup discGroup = dao.get(DiscGroup.class, id);
        if (dao.get(DiscGroup.class, id) == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[删除列表失败][指定的列表Id不存在][Id={}]", id);
            }
            return errorMessage("指定的列表Id不存在");
        }

        infoRequest("[删除列表开始][列表={}]", discGroup.getTitle());
        Set<Disc> discs = discGroup.getDiscs();
        if (LOGGER.isDebugEnabled()) {
            discs.forEach(disc -> {
                LOGGER.debug("[记录列表中的碟片][列表={}][碟片={}]", discGroup.getTitle(), disc.getLogName());
            });
        }

        int discCount = discs.size();
        discs.clear();
        dao.delete(discGroup);

        if (LOGGER.isDebugEnabled()) {
            infoRequest("[删除列表成功][该列表共有碟片{}个]", discCount);
        }
        jmsMessage.danger("[用户=%s][删除列表成功][列表=%s]", getUserName(), discGroup.getTitle());
        return objectResult(discGroup.toJSON());
    }

}
