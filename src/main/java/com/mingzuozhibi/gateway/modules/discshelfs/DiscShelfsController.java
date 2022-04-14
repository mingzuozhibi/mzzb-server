package com.mingzuozhibi.gateway.modules.discshelfs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.model.Content;
import com.mingzuozhibi.gateway.connect.Connect;
import com.mingzuozhibi.gateway.modules.Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
public class DiscShelfsController extends BaseController {

    @Autowired
    private Connect connect;

    @Resource(name = "redisTemplate")
    private SetOperations<String, String> setOps;

    @Transactional
    @GetMapping(value = "/gateway/discShelfs", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int pageSize) {

        if (pageSize > 40) {
            return errorResult("pageSize不能超过40");
        }

        String uri = String.format("/discShelfs?page=%d&pageSize=%d", page, pageSize);
        Content content = Content.parse(connect.waitRequest(Module.DISC_SHELFS, uri));
        if (content.isSuccess()) {
            matchTracked(content.getArray());
        }
        return content.getRoot().toString();
    }

    private void matchTracked(JsonArray discShelfs) {
        discShelfs.forEach(element -> {
            JsonObject discShelf = element.getAsJsonObject();
            discShelf.addProperty("tracked", isTracked(discShelf));
        });
    }

    private Boolean isTracked(JsonObject discShelf) {
        String asin = discShelf.get("asin").getAsString();
        return setOps.isMember("disc.track", asin);
    }

}
