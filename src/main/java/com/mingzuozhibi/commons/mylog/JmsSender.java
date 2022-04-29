package com.mingzuozhibi.commons.mylog;

import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsEnums.Type;
import com.mingzuozhibi.modules.core.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Slf4j
@Component
public class JmsSender {

    @Autowired
    private JmsTemplate template;

    public void info(Name name, Type type, String text) {
        Message message = new Message(name, type, text);
        log.info("JMS -> listen.message: {}", message);
        send("listen.message", GSON.toJson(message));
    }

    public void send(String destination, String json) {
        for (int i = 0; i < 3; i++) {
            try {
                template.convertAndSend(destination, json);
                break;
            } catch (JmsException e) {
                String format = "convertAndSend(destination=%s, json=%s)";
                log.debug(String.format(format, destination, json), e);
            }
        }
    }

    public JmsLogger bind(Name name) {
        return new JmsLogger(name, this);
    }

}
