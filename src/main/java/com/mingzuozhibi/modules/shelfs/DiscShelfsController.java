package com.mingzuozhibi.modules.shelfs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.modules.connect.Connect;
import com.mingzuozhibi.modules.disc.DiscRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import static com.mingzuozhibi.modules.connect.Module.DISC_SHELFS;

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
        Result<String> bodyResult = connect.waitResult(DISC_SHELFS, uri);
        if (bodyResult.hasError()) {
            return errorResult(bodyResult.getError());
        }
        JsonObject object = gson.fromJson(bodyResult.getData(), JsonObject.class);
        if (object.get("success").getAsBoolean()) {
            matchTracked(object.get("data").getAsJsonArray());
        }
        return object.toString();
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
