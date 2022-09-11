package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Function;

@Service
@LoggerBind(Name.DEFAULT)
public class VarableService extends BaseSupport {

    @Autowired
    private VarableRepository varableRepository;

    @Transactional
    public <T> VarBean<T> create(String key, T value, Function<T, String> format, Function<String, T> parse) {
        Optional<Varable> byKey = this.varableRepository.findByKey(key);
        if (byKey.isPresent()) {
            T load = parse.apply(byKey.get().getContent());
            return new VarBean<>(key, load, format, varableRepository);
        } else {
            varableRepository.save(new Varable(key, format.apply(value)));
            return new VarBean<>(key, value, format, varableRepository);
        }
    }

    public VarBean<Integer> createInteger(String key, int value) {
        return create(key, value, String::valueOf, Integer::valueOf);
    }

    public VarBean<Boolean> createBoolean(String key, boolean value) {
        return create(key, value, String::valueOf, Boolean::valueOf);
    }

}
