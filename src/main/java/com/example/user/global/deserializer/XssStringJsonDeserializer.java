package com.example.user.global.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class XssStringJsonDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) return null;

        return sanitize(value);
    }

    private String sanitize(String value) {
        return value
                .replaceAll("(?i)<script", "&lt;script")
                .replaceAll("(?i)</script>", "&lt;/script&gt;");
    }
}
