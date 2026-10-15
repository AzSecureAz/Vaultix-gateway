package com.vaultix.gateway.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vaultix.cors")
public record CorsProperties(
        @DefaultValue({"http://localhost:3000", "http://localhost:5173"})
        List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"})
        List<String> allowedMethods,
        @DefaultValue({"*"})
        List<String> allowedHeaders,
        @DefaultValue("true")
        boolean allowCredentials,
        @DefaultValue("3600")
        long maxAge) {

}
