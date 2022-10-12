package com.mingzuozhibi.commons.base;

import com.mingzuozhibi.commons.domain.ResultPage;
import org.springframework.data.domain.*;

public abstract class PageController extends BaseController {

    protected <T> String pageResult(Page<T> page) {
        var pageable = page.getPageable();
        var resultPage = new ResultPage(
            pageable.getPageSize(),
            pageable.getPageNumber() + 1,
            page.getTotalElements());
        return pageResult(page.getContent(), resultPage);
    }

    protected PageRequest pageRequest(Pageable pageable, Sort defaultSort) {
        var pageNumber = pageable.previousOrFirst().getPageNumber();
        var pageSize = pageable.getPageSize();
        var sort = pageable.getSortOr(defaultSort);
        return PageRequest.of(pageNumber, pageSize, sort);
    }

}
