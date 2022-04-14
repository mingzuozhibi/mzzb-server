package com.mingzuozhibi.gateway.message;

import com.google.gson.JsonObject;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;

@Component
public class MessageService {

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    public void pushModuleMsg(String moduleName, JsonObject data) {
        data.addProperty("acceptOn", Instant.now().toEpochMilli());
        String message = data.toString();
        MessageType msgType = MessageType.parse(data.get("type").getAsString());
        for (MessageType logType : MessageType.values()) {
            if (logType.match(msgType)) {
                listOps.leftPush(keyOfMsgs(moduleName, logType), message);
                listOps.trim(keyOfMsgs(moduleName, logType), 0, 7999);
            }
        }
    }

    public List<String> findModuleMsg(String moduleName, MessageType messageType, int page, int pageSize) {
        int start = (page - 1) * pageSize;
        int end = page * pageSize - 1;
        return listOps.range(keyOfMsgs(moduleName, messageType), start, end);
    }

    public Long countModuleMsg(String moduleName, MessageType messageType) {
        return listOps.size(keyOfMsgs(moduleName, messageType));
    }

    private String keyOfMsgs(String moduleName, MessageType messageType) {
        return String.format("%s.msgs.%s", moduleName, messageType.name());
    }

}
