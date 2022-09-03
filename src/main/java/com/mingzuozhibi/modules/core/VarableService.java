package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@LoggerBind(Name.DEFAULT)
public class VarableService extends BaseSupport {

    @Autowired
    private VarableRepository varableRepository;

    @Transactional
    public Optional<String> findByKey(String key) {
        return varableRepository.findByKey(key).map(Varable::getContent);
    }

    @Transactional
    public Optional<Integer> findIntegerByKey(String key) {
        return findByKey(key).map(Integer::parseInt);
    }

    @Transactional
    public void saveOrUpdate(String key, String content) {
        Optional<Varable> byKey = varableRepository.findByKey(key);
        if (byKey.isEmpty()) {
            varableRepository.save(new Varable(key, content));
        } else {
            byKey.get().setContent(content);
        }
    }

}
