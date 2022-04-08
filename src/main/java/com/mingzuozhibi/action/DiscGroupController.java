package com.mingzuozhibi.action;

import com.mingzuozhibi.commons.BaseController;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class DiscGroupController extends BaseController {

    @Autowired
    private JmsMessage jmsMessage;

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
