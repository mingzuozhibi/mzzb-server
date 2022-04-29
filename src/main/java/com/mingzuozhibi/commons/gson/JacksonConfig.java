package com.mingzuozhibi.commons.gson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.*;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.*;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJackson() {
        return builder -> {
            builder.serializationInclusion(Include.NON_NULL);
            SimpleModule module = new SimpleModule();

            withLocalDateTime(module);
            withLocalDate(module);
            withInsatnt(module);

            builder.modulesToInstall(module);
        };
    }

    private void withLocalDateTime(SimpleModule module) {
        module.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(toEpochMilli(value));
            }
        });
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return ofEpochMilli(p.getLongValue());
            }
        });
    }

    private void withLocalDate(SimpleModule module) {
        module.addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
            public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.format(fmtDate));
            }
        });
        module.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return LocalDate.parse(p.getText(), fmtDate);
            }
        });
    }

    private void withInsatnt(SimpleModule module) {
        module.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.toEpochMilli());
            }
        });
        module.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Instant.ofEpochMilli(p.getLongValue());
            }
        });
    }

}
