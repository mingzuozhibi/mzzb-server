package com.mingzuozhibi.commons;

import com.mingzuozhibi.commons.gson.GsonIgnored;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

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

}
