package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseEntity;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.time.Instant;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class History extends BaseEntity {

    private static final long serialVersionUID = 100L;

    @Column(nullable = false, length = 20, unique = true)
    private String asin;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private boolean tracked;

    @Column(nullable = false)
    private Instant createOn;

}