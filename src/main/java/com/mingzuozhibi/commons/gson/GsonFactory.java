package com.mingzuozhibi.commons.gson;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public abstract class GsonFactory {

    public static final Gson GSON = GsonFactory.createGson();

    public static Gson createGson() {
        GsonBuilder gson = new GsonBuilder();
        gson.setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(Ignore.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        });
        gson.registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
            @Override
            public void write(JsonWriter writer, Instant instant) throws IOException {
                if (instant != null) {
                    writer.value(instant.toEpochMilli());
                } else {
                    writer.nullValue();
                }
            }

            @Override
            public Instant read(JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                } else {
                    return Instant.ofEpochMilli(reader.nextLong());
                }
            }
        });
        return gson.create();
    }

}
