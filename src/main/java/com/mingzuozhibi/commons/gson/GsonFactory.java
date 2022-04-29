package com.mingzuozhibi.commons.gson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.*;
import com.mingzuozhibi.commons.gson.adapter.*;

import java.time.*;

public abstract class GsonFactory {

    public static final Gson GSON = GsonFactory.createGson();

    private static Gson createGson() {
        GsonBuilder gson = new GsonBuilder();
        gson.setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(JsonIgnore.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        });
        gson.registerTypeAdapter(Instant.class, new AdapterOfInstant());
        gson.registerTypeAdapter(LocalDate.class, new AdapterOfLocalDate());
        gson.registerTypeAdapter(LocalDateTime.class, new AdapterOfLocalDateTime());
        return gson.create();
    }

}
