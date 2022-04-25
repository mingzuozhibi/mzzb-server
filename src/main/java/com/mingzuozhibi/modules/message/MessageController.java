package com.mingzuozhibi.modules.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    @GetMapping(value = "/api/messages/{moduleName}", produces = MEDIA_TYPE)
    public String findMessages(
        @PathVariable String moduleName,
        @RequestParam(defaultValue = "info") MessageType type,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int pageSize) {

        List<String> moduleMsg = messageService.findMessages(moduleName, type, page, pageSize);
        Long count = messageService.countMessage(moduleName, type);

        JsonArray array = new JsonArray();
        for (String msg : moduleMsg) {
            array.add(gson.fromJson(msg, JsonObject.class));
        }
        return gsonResult(array, gsonPage(page, pageSize, count));
    }

}
