package com.mingzuozhibi.modules.core;

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

    public void saveMessage(String name, JsonObject object) {
        object.addProperty("acceptOn", Instant.now().toEpochMilli());
        String mJson = object.toString();
        String mType = object.get("type").getAsString();
        MessageType toMatch = MessageType.parse(mType);
        for (MessageType type : MessageType.values()) {
            if (type.match(toMatch)) {
                String key = keyOfMsgs(name, type);
                listOps.leftPush(key, mJson);
                listOps.trim(key, 0, 7999);
            }
        }
    }

    public List<String> findMessages(String name, MessageType type, int page, int size) {
        int start = (page - 1) * size;
        int end = page * size - 1;
        return listOps.range(keyOfMsgs(name, type), start, end);
    }

    public Long countMessage(String name, MessageType type) {
        return listOps.size(keyOfMsgs(name, type));
    }

    private String keyOfMsgs(String name, MessageType type) {
        return String.format("%s.msgs.%s", name, type.name());
    }

}
