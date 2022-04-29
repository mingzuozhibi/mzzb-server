package com.mingzuozhibi.commons.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 100L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @JsonIgnore
    private Long version;

}
