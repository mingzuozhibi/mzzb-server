package com.mingzuozhibi.modules.core;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.domain.ResultPage;
import com.mingzuozhibi.commons.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    @GetMapping(value = "/api/messages/{moduleName}", produces = MEDIA_TYPE)
    public String findMessages(@PathVariable String moduleName,
                               @RequestParam(defaultValue = "info") MessageType type,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "50") int pageSize) {
        List<JsonObject> messages = messageService.findMessages(moduleName, type, page, pageSize).stream()
            .map(json -> GsonFactory.GSON.fromJson(json, JsonObject.class))
            .collect(Collectors.toList());
        Long count = messageService.countMessage(moduleName, type);
        return pageResult(messages, new ResultPage(page, pageSize, count));
    }

}
