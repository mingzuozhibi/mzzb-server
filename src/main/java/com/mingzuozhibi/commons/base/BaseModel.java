package com.mingzuozhibi.commons.base;

import com.mingzuozhibi.commons.gson.GsonIgnored;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@MappedSuperclass
public abstract class BaseModel implements Serializable {

    private Long id;
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    @GsonIgnored
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    protected Long toEpochMilli(LocalDateTime dateTime) {
        return Optional.ofNullable(dateTime)
            .map(date -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .orElse(0L);
    }

}
