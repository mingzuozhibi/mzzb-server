package com.mingzuozhibi.commons.amqp;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseKeys.Type;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AmqpLogger {

    public AmqpLogger(Name name, Type type, String text) {
        this.name = name;
        this.type = type;
        this.text = text;
        this.createOn = Instant.now();
    }

    private Name name;

    private Type type;

    private String text;

    private Instant createOn;

    public String toString() {
        return "[%s][%s][%s]".formatted(type, name, text);
    }

}
