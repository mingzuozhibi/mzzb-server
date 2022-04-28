package com.mingzuozhibi.commons.gson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJackson() {
        return builder -> {
            builder.serializationInclusion(Include.NON_NULL);
            builder.modulesToInstall(getSimpleModule());
        };
    }

    private SimpleModule getSimpleModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.toEpochMilli());
            }
        });
        module.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                return Instant.ofEpochMilli(p.getLongValue());
            }
        });
        module.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
        });
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                return Instant.ofEpochMilli(p.getLongValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        });
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        module.addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
            public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.format(formatter));
            }
        });
        module.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                return LocalDate.parse(p.getText(), formatter);
            }
        });
        return module;
    }

}
