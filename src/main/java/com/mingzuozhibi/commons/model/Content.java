package com.mingzuozhibi.commons.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

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

    private JsonObject root;

    public Content(String content) {
        parseContent(content);
    }

    private void parseContent(String content) {
        root = GSON.fromJson(content, JsonObject.class);
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

    public JsonObject getRoot() {
        return root;
    }

}
