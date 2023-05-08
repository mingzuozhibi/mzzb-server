package com.mingzuozhibi.commons.gson;

import com.google.gson.JsonArray;

import java.util.List;

public abstract class GsonUtils {

    public static JsonArray buildArray(List<String> values) {
        var array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

}
