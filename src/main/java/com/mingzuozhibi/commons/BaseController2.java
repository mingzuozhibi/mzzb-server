package com.mingzuozhibi.commons;

import com.google.gson.Gson;
import com.mingzuozhibi.commons.model.BaseResult;
import com.mingzuozhibi.commons.model.DataResult;
import com.mingzuozhibi.commons.model.PageResult;
import com.mingzuozhibi.commons.utils.ServletUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

public class BaseController2 extends ServletUtils {

    @Autowired
    protected Gson gson;

    protected String errorResult(String error) {
        return gson.toJson(new BaseResult(error));
    }

    protected <T> String dataResult(T data) {
        return gson.toJson(new DataResult<T>(data));
    }

    protected <T> String pageResult(Page<T> page) {
        return gson.toJson(new PageResult<T>(page));
    }

    protected <T> String baseResult(BaseResult baseResult) {
        return gson.toJson(baseResult);
    }

}
