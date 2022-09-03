package com.mingzuozhibi.commons.domain;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResultPage {

    private int pageSize;
    private int currentPage;
    private long totalElements;

}
