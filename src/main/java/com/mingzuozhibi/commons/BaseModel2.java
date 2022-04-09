package com.mingzuozhibi.commons;

import com.mingzuozhibi.commons.gson.GsonIgnored;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseModel2 implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @GsonIgnored
    @Version
    private Long version;

    protected Long toEpochMilli(LocalDateTime dateTime) {
        return Optional.ofNullable(dateTime)
            .map(date -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .orElse(0L);
    }

}
