package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseEntity;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseKeys.Type;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Message extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 100L;

    public Message(Name name, Type type, String text) {
        this.name = name;
        this.type = type;
        this.text = text;
        this.createOn = Instant.now();
    }

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Name name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.ORDINAL)
    private Type type;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private Instant createOn;

    @Column(nullable = false)
    private Instant acceptOn;

    public Message withAccept() {
        this.setAcceptOn(Instant.now());
        return this;
    }

    public String toString() {
        return "[%s][%s][%s]".formatted(type, name, text);
    }

}
