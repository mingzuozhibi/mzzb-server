package com.mingzuozhibi.commons.base;

import com.mingzuozhibi.commons.domain.ResultPage;
import org.springframework.data.domain.*;

public abstract class PageController extends BaseController {

    protected <T> String pageResult(Page<T> page) {
        var p = page.getPageable();
        var pageSize = p.getPageSize();
        var currentPage = p.getPageNumber() + 1;
        var totalElements = page.getTotalElements();
        return pageResult(page.getContent(), new ResultPage(pageSize, currentPage, totalElements));
    }

    protected PageRequest pageRequest(Pageable pageable, Sort defaultSort) {
        var pageNumber = pageable.previousOrFirst().getPageNumber();
        var pageSize = pageable.getPageSize();
        var sort = pageable.getSortOr(defaultSort);
        return PageRequest.of(pageNumber, pageSize, sort);
    }

}
