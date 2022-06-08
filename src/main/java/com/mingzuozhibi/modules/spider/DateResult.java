package com.mingzuozhibi.modules.spider;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DateResult {

    private LocalDateTime date;
    private List<Content> result;

    public DateResult(List<Content> result) {
        this.date = LocalDateTime.now();
        this.result = result;
    }

    public int count() {
        return result == null ? 0 : result.size();
    }

}
