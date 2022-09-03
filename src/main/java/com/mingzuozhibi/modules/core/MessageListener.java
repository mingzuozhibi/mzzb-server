package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.mingzuozhibi.commons.base.BaseKeys.MODULE_MESSAGE;

@Slf4j
@Component
@LoggerBind(Name.SERVER_CORE)
public class MessageListener extends BaseSupport {

    @Autowired
    private MessageRepository messageRepository;

    @RabbitListener(queues = MODULE_MESSAGE)
    public void moduleMessage(String json) {
        Message message = gson.fromJson(json, Message.class);
        if (message.getText().length() > 1000) {
            log.info("moduleMessage(json=%s)".formatted(message));
            message.setText(message.getText().substring(0, 1000));
        }
        try {
            messageRepository.save(message.withAccept());
        } catch (Exception e) {
            log.warn("moduleMessage(json=%s): %s".formatted(message, e));
        }
    }

}
