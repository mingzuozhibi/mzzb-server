package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseKeys.Type;
import com.mingzuozhibi.commons.base.PageController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MessageController extends PageController {

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping(value = "/api/messages/{name}", produces = MEDIA_TYPE)
    public String findAll(@PathVariable Name name,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) List<Type> types,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {

        var pageRequest = PageRequest.of(page - 1, size, Sort.by(Order.desc("id")));
        var messages = messageRepository.findBy(name, types, search, pageRequest);
        return pageResult(messages);

    }

}
