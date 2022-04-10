package com.mingzuozhibi.commons.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PageResult<T> extends BaseResult {

    private List<T> data;
    private PageObj page;

    public PageResult(Page<T> page) {
        setSuccess(true);
        this.data = page.getContent();
        this.page = new PageObj(page);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PageObj {
        private int pageSize;
        private int currentPage;
        private long totalElements;

        public PageObj(Page<?> page) {
            Pageable pageable = page.getPageable();
            this.pageSize = pageable.getPageSize();
            this.currentPage = pageable.getPageNumber() + 1;
            this.totalElements = page.getTotalElements();
        }
    }

}
