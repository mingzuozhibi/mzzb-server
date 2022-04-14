package com.mingzuozhibi.commons.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.gson.GsonFactory;

public class Content {

    public static Content parse(Result<String> bodyResult) {
        if (!bodyResult.isUnfinished()) {
            return new Content(bodyResult.getContent());
        } else {
            JsonObject errorResult = new JsonObject();
            errorResult.addProperty("success", false);
            errorResult.addProperty("message", bodyResult.formatError());
            return new Content(errorResult.toString());
        }
    }

    private static Gson gson = GsonFactory.createGson();

    private JsonObject root;

    public Content(String content) {
        parseContent(content);
    }

    private void parseContent(String content) {
        root = gson.fromJson(content, JsonObject.class);
    }

    public boolean isSuccess() {
        return root.get("success").getAsBoolean();
    }

    public String getMessage() {
        return root.get("message").getAsString();
    }

    public JsonObject getObject() {
        return getData().getAsJsonObject();
    }

    public JsonArray getArray() {
        return getData().getAsJsonArray();
    }

    public JsonElement getData() {
        return root.get("data");
    }

    public JsonObject getPage() {
        return root.get("page").getAsJsonObject();
    }

    public JsonObject getRoot() {
        return root;
    }

    public Page parsePage() {
        return gson.fromJson(getPage(), Page.class);
    }

    public <T> T parseData(Class<T> dataType) {
        return gson.fromJson(getData(), dataType);
    }

}
