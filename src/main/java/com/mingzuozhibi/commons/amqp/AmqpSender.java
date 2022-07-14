package com.mingzuozhibi.commons.amqp;

import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.AmqpEnums.Type;
import com.mingzuozhibi.commons.amqp.logger.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.mingzuozhibi.commons.amqp.AmqpEnums.MODULE_MESSAGE;
import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Slf4j
@Component
public class AmqpSender {

    @Autowired
    private AmqpTemplate template;

    public void info(Name name, Type type, String text) {
        AmqpLogger amqpLogger = new AmqpLogger(name, type, text);
        log.info("JMS -> %s msg=%s".formatted(MODULE_MESSAGE, amqpLogger));
        send(MODULE_MESSAGE, GSON.toJson(amqpLogger));
    }

    public void send(String destination, String json) {
        for (int i = 0; i < 3; i++) {
            try {
                template.convertAndSend(destination, json);
                break;
            } catch (AmqpException e) {
                log.debug("convertAndSend(destination=%s, json=%s)".formatted(destination, json), e);
            }
        }
    }

    public Logger bind(Name name) {
        return new Logger(name, this);
    }

}
