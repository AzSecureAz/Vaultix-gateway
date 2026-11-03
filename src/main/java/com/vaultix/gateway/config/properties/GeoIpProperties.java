package com.vaultix.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "vaultix.geoip")
public record GeoIpProperties(
        @NotBlank(message = "GeoIP database path is required")
        @DefaultValue("../Vaultix-infra/geoip/GeoLite2-City.mmdb")
        String databasePath) {

}
