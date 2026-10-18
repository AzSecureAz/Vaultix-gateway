package com.vaultix.gateway.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "vaultix.cors")
public record CorsProperties(
        @NotEmpty(message = "Allowed origins cannot be empty")
        @DefaultValue({"http://localhost:3000", "http://localhost:5173"})
        List<@NotBlank(message = "Allowed origin cannot be blank")
        @Size(max = 1024, message = "Allowed origin cannot exceed 1024 characters") String> allowedOrigins,
        @NotEmpty(message = "Allowed methods cannot be empty")
        @DefaultValue({"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"})
        List<@NotBlank(message = "Allowed method cannot be blank")
        @Size(max = 64, message = "Allowed method cannot exceed 64 characters") String> allowedMethods,
        @NotEmpty(message = "Allowed headers cannot be empty")
        @DefaultValue({"*"})
        List<@NotBlank(message = "Allowed header cannot be blank")
        @Size(max = 256, message = "Allowed header cannot exceed 256 characters") String> allowedHeaders,
        @DefaultValue("true")
        boolean allowCredentials,
        @PositiveOrZero(message = "Max age must be greater than or equal to 0")
        @DefaultValue("3600")
        long maxAge) {

}
