package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseKeys.Type;
import com.mingzuozhibi.commons.base.PageController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;

@Transactional
@RestController
public class MessageController extends PageController {

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping(value = "/api/messages/{name}", produces = MEDIA_TYPE)
    public String findAll(@PathVariable Name name,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) List<Type> types,
                          @RequestParam(required = false) LocalDate start,
                          @RequestParam(required = false) LocalDate end,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {

        var messages = messageRepository.findBy(name, types, search,
            safeInstance(start, LocalTime.of(0, 0, 0)),
            safeInstance(end, LocalTime.of(23, 59, 59)),
            PageRequest.of(page - 1, size, Sort.by(Order.desc("id"))));
        return pageResult(messages);
    }

    private static Instant safeInstance(LocalDate date, LocalTime time) {
        if (date == null) return null;
        return toInstant(LocalDateTime.of(date, time));
    }

}
