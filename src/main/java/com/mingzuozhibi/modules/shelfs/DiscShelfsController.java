package com.mingzuozhibi.modules.shelfs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.model.Content;
import com.mingzuozhibi.modules.connect.Connect;
import com.mingzuozhibi.modules.connect.Module;
import com.mingzuozhibi.modules.disc.DiscRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DiscShelfsController extends BaseController {

    @Autowired
    private Connect connect;

    @Autowired
    private DiscRepository discRepository;

    @Transactional
    @GetMapping(value = "/api/discShelfs", produces = MEDIA_TYPE)
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
            String asin = discShelf.get("asin").getAsString();
            boolean exists = discRepository.existsByAsin(asin);
            discShelf.addProperty("tracked", exists);
        });
    }

}
