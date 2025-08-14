package com.arkioner.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class ConfigLoader {
    public static ServerProperties loadServerConfig(ObjectMapper objectMapper){
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("serverProperties.json")) {
            if (input == null) {
                throw new RuntimeException("Server properties file not found!");
            }
            return objectMapper.readValue(input, ServerProperties.class);
        } catch (NumberFormatException | IOException se) {
            throw new RuntimeException("Failed to load server.properties", se);
        }
    }

    public record ServerProperties(
            HttpServerConfig http
    ){}

    public record HttpServerConfig(
            Integer port
    ){}
}
