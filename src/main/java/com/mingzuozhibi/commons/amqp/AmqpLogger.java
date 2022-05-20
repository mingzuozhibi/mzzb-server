package com.mingzuozhibi.commons.amqp;

import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.AmqpEnums.Type;
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
        return String.format("[%s][%s][%s]", type, name, text);
    }

}
