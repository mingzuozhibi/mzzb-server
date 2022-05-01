package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.mylog.JmsEnums;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsEnums.Type;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
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

        return pageResult(messageRepository.findAll((Specification<Message>) (root, query, cb) -> {
            ArrayList<Predicate> array = new ArrayList<>();
            array.add(cb.equal(root.get("name"), name));
            if (StringUtils.isNotBlank(search)) {
                array.add(cb.like(root.get("text"), "%" + search + "%"));
            }
            if (types != null && !types.isEmpty() && types.size() < JmsEnums.Type.values().length) {
                array.add(cb.in(root.get("type")).value(types));
            }
            return query.where(array.toArray(new Predicate[0])).getRestriction();
        }, PageRequest.of(page - 1, size, Sort.by(Order.desc("id")))));

    }

}
