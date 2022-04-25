package com.mingzuozhibi.commons.gson.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.*;

import java.io.IOException;
import java.time.LocalDate;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;

public class AdapterOfLocalDate extends TypeAdapter<LocalDate> {

    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
        if (value != null) {
            out.value(value.format(fmtDate));
        } else {
            out.nullValue();
        }
    }

    @Override
    public LocalDate read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            return LocalDate.parse(in.nextString(), fmtDate);
        }
    }

}
