package com.mingzuozhibi.modules.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

        List<String> moduleMsg = messageService.findModuleMsg(moduleName, type, page, pageSize);
        Long count = messageService.countModuleMsg(moduleName, type);

        JsonArray root = new JsonArray();
        moduleMsg.forEach(msg -> root.add(gson.fromJson(msg, JsonObject.class)));
        return objectResult(root, buildPage(page, pageSize, count));
    }

}
