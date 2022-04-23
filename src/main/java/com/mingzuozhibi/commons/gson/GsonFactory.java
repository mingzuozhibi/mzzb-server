package com.mingzuozhibi.commons.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.gson.adapter.AdapterOfInstant;
import com.mingzuozhibi.commons.gson.adapter.AdapterOfLocalDate;
import com.mingzuozhibi.commons.gson.adapter.AdapterOfLocalDateTime;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class GsonFactory {

    public static final Gson GSON = GsonFactory.createGson();

    public static Gson createGson() {
        GsonBuilder gson = new GsonBuilder();
        gson.setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(GsonIgnored.class) != null;
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

    public static Type getType(Type rawType, Type... typeArguments) {
        return TypeToken.getParameterized(rawType, typeArguments).getType();
    }

    public static <T> T fromJson(String json, Type rawType, Type... typeArguments) {
        return GSON.fromJson(json, getType(rawType, typeArguments));
    }

}
