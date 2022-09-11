package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseEntity;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serial;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Varable extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 100L;

    public Varable(String key, String content) {
        this.key = key;
        this.content = content;
    }

    @Column(nullable = false, unique = true)
    public String key;

    @Column
    public String content;

}
