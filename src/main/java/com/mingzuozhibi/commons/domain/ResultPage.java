package com.mingzuozhibi.commons.domain;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Getter
public class ResultPage {

    public ResultPage(Page<?> page) {
        Pageable pageable = page.getPageable();
        this.pageSize = pageable.getPageSize();
        this.currentPage = pageable.getPageNumber() + 1;
        this.totalElements = page.getTotalElements();
    }

    private final int pageSize;

    private final int currentPage;

    private final long totalElements;

}
