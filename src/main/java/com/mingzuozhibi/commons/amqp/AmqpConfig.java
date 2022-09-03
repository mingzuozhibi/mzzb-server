package com.mingzuozhibi.commons.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.mingzuozhibi.commons.base.BaseKeys.*;

@Configuration
public class AmqpConfig {

    @Bean
    public Queue fetchTaskStart() {
        return new Queue(FETCH_TASK_START);
    }

    @Bean
    public Queue fetchTaskDone1() {
        return new Queue(FETCH_TASK_DONE1);
    }

    @Bean
    public Queue fetchTaskDone2() {
        return new Queue(FETCH_TASK_DONE2);
    }

    @Bean
    public Queue historyFinish() {
        return new Queue(HISTORY_FINISH);
    }

    @Bean
    public Queue contentFinish() {
        return new Queue(CONTENT_FINISH);
    }

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

}
