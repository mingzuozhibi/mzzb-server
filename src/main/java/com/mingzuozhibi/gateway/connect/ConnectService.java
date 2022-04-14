package com.mingzuozhibi.gateway.connect;

import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Component
public class ConnectService {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOps;

    public void setModuleAddr(String moduleName, String moduleAddr) {
        valueOps.set(keyOfAddr(moduleName), moduleAddr);
    }

    public Optional<String> getModuleAddr(String moduleName) {
        return Optional.ofNullable(valueOps.get(keyOfAddr(moduleName)));
    }

    public Optional<String> getHttpPrefix(String moduleName) {
        return getModuleAddr(moduleName).map("http://"::concat);
    }

    private String keyOfAddr(String moduleName) {
        return moduleName + ".addr";
    }

}
