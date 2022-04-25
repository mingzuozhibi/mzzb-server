package com.mingzuozhibi.commons.gson.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.*;

import java.io.IOException;
import java.time.Instant;

public class AdapterOfInstant extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value != null) {
            out.value(value.toEpochMilli());
        } else {
            out.nullValue();
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            return Instant.ofEpochMilli(in.nextLong());
        }
    }

}
