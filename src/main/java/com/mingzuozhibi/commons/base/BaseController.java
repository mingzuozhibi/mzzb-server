package com.mingzuozhibi.commons.base;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.commons.result.ResultSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

public abstract class BaseController extends ResultSupport {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    protected Gson gson;

    @Autowired
    protected JmsMessage jmsMessage;

    @ResponseBody
    @ExceptionHandler
    public String errorHandler(Exception e) {
        return errorResult(e.toString());
    }

    public String gsonResult(JsonElement data, JsonElement page) {
        JsonObject root = new JsonObject();
        root.addProperty("success", true);
        root.add("data", data);
        root.add("page", page);
        return root.toString();
    }

    public JsonElement gsonPage(long currentPage, long pageSize, long totalElements) {
        JsonObject object = new JsonObject();
        object.addProperty("pageSize", pageSize);
        object.addProperty("currentPage", currentPage);
        object.addProperty("totalElements", totalElements);
        return object;
    }

}
