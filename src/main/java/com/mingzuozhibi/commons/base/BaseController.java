package com.mingzuozhibi.commons.base;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.domain.Result;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class BaseController extends BaseSupport {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    @ResponseBody
    @ExceptionHandler
    public String errorHandler(Exception e) {
        return errorResult(e.toString());
    }

    protected <T> String dataResult(T data) {
        return GSON.toJson(Result.ofData(data));
    }

    protected <T> String pageResult(Page<T> page) {
        return GSON.toJson(Result.ofPage(page));
    }

    protected String gsonResult(JsonElement data, JsonElement page) {
        JsonObject root = new JsonObject();
        root.addProperty("success", true);
        root.add("data", data);
        root.add("page", page);
        return root.toString();
    }

    protected JsonElement gsonPage(long currentPage, long pageSize, long totalElements) {
        JsonObject object = new JsonObject();
        object.addProperty("pageSize", pageSize);
        object.addProperty("currentPage", currentPage);
        object.addProperty("totalElements", totalElements);
        return object;
    }

}
