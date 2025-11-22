package com.example.user.global.config;

import com.example.user.global.deserializer.XssStringJsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Module xssSanitizingModule(XssStringJsonDeserializer xssDeserializer) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, xssDeserializer);
        return module;
    }
}
