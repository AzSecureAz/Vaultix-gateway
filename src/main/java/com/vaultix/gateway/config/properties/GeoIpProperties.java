package com.vaultix.gateway.config.properties;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "vaultix.geoip")
public record GeoIpProperties(
        @NotNull(message = "GeoIP database path is required")
        @DefaultValue("../infra/geoip/GeoLite2-City.mmdb")
        Path databasePath) {

}
