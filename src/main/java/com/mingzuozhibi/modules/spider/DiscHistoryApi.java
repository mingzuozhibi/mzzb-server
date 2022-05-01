package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.modules.core.Connect;
import com.mingzuozhibi.modules.disc.DiscRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import static com.mingzuozhibi.modules.core.Connect.Module.DISC_SHELFS;

@Slf4j
@RestController
public class DiscHistoryApi extends BaseController {

    @Autowired
    private Connect connect;

    @Autowired
    private DiscRepository discRepository;

    public Result<JsonObject> findHistory(int page, int size) {
        String uri = String.format("/discShelfs?page=%d&pageSize=%d", page, size);
        Result<String> bodyResult = connect.waitResult(DISC_SHELFS, uri);
        if (bodyResult.hasError()) {
            return Result.ofError(bodyResult.getMessage());
        }
        JsonObject object = gson.fromJson(bodyResult.getData(), JsonObject.class);
        if (object.get("success").getAsBoolean()) {
            matchTracked(object.get("data").getAsJsonArray());
        }
        return Result.ofData(object);
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
