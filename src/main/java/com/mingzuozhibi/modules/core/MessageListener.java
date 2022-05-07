package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsBind;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static com.mingzuozhibi.commons.mylog.JmsEnums.MODULE_MESSAGE;

@Slf4j
@Component
@JmsBind(Name.SERVER_CORE)
public class MessageListener extends BaseSupport {

    @Autowired
    private MessageRepository messageRepository;

    @JmsListener(destination = MODULE_MESSAGE)
    public void moduleMessage(String json) {
        Message message = gson.fromJson(json, Message.class);
        if (message.getText().length() > 1000) {
            message.setText(message.getText().substring(0, 1000));
            log.info("saveMessage({})", message);
        }
        try {
            messageRepository.save(message.withAccept());
        } catch (Exception e) {
            log.warn("saveMessage({}): {}", message, e);
        }
    }

    @JmsListener(destination = "ActiveMQ.DLQ")
    public void ActiveMQ_DLQ(String json) {
        bind.warning("JMS <- ActiveMQ.DLQ: %s", json);
    }

}
