package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.BaseController2;
import com.mingzuozhibi.commons.check.CheckUtils;
import com.mingzuozhibi.modules.disc.DiscGroup.ViewType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class DiscGroupController2 extends BaseController2 {

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
            return CheckUtils.paramNoExists("列表索引");
        }
        return dataResult(byKey.get());
    }

}
