package com.mingzuozhibi.commons.base;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.result.ResultSupport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Objects;

public abstract class BaseController extends ResultSupport {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    @ResponseBody
    @ExceptionHandler
    public String errorHandler(Exception e) {
        return errorResult(e.toString());
    }

    public String objectResult(JsonElement data, JsonElement page) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(page);
        JsonObject root = new JsonObject();
        root.addProperty("success", true);
        root.add("data", data);
        root.add("page", page);
        return root.toString();
    }

    public JsonElement buildPage(long currentPage, long pageSize, long totalElements) {
        JsonObject object = new JsonObject();
        object.addProperty("pageSize", pageSize);
        object.addProperty("currentPage", currentPage);
        object.addProperty("totalElements", totalElements);
        return object;
    }

}
