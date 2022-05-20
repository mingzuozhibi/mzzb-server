package com.mingzuozhibi.commons.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.mingzuozhibi.commons.amqp.AmqpEnums.*;

@Configuration
public class AmqpConfig {

    @Bean
    public Queue moduleMessage() {
        return new Queue(MODULE_MESSAGE);
    }

    @Bean
    public Queue contentSearch() {
        return new Queue(CONTENT_SEARCH);
    }

    @Bean
    public Queue contentReturn() {
        return new Queue(CONTENT_RETURN);
    }

    @Bean
    public Queue historyUpdate() {
        return new Queue(HISTORY_UPDATE);
    }

    @Bean
    public Queue historyFinish() {
        return new Queue(HISTORY_FINISH);
    }

    @Bean
    public Queue needUpdateAsins() {
        return new Queue(NEED_UPDATE_ASINS);
    }

    @Bean
    public Queue doneUpdateDiscs() {
        return new Queue(DONE_UPDATE_DISCS);
    }

    @Bean
    public Queue prevUpdateDiscs() {
        return new Queue(PREV_UPDATE_DISCS);
    }

    @Bean
    public Queue lastUpdateDiscs() {
        return new Queue(LAST_UPDATE_DISCS);
    }

}
