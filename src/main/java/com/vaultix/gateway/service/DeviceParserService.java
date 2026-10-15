package com.vaultix.gateway.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;
import ua_parser.Client;
import ua_parser.Parser;

@Slf4j
@Service
public class DeviceParserService {

    private final Parser uaParser;
    private final Cache<String, String> deviceCache;

    public DeviceParserService() {
        this.uaParser = new Parser();
        this.deviceCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(24, TimeUnit.HOURS)
                .build();
    }

    /**
     * Parses client device headers into a concise summary: 'Browser on OS'
     * (e.g., 'Chrome 122 on Windows').
     *
     * @param userAgent the User-Agent header value
     * @param secChUaPlatform optional Sec-CH-UA-Platform header (e.g.,
     * "Windows")
     * @return a clean device description, or 'Unknown'
     */
    public String resolveDeviceInfo(String userAgent, String secChUaPlatform) {
        if ((userAgent == null || userAgent.isBlank()) && (secChUaPlatform == null || secChUaPlatform.isBlank())) {
            return "Unknown";
        }

        String cacheKey = (userAgent != null ? userAgent.trim() : "") + "|"
                + (secChUaPlatform != null ? secChUaPlatform.trim() : "");

        return deviceCache.get(cacheKey, k -> parseDevice(userAgent, secChUaPlatform));
    }

    private String parseDevice(String userAgent, String secChUaPlatform) {

        try {
            String browser = null;
            String os = null;

            if (userAgent != null && !userAgent.isBlank()) {
                Client client = uaParser.parse(userAgent);

                if (client.userAgent != null && client.userAgent.family != null
                        && !"Other".equalsIgnoreCase(client.userAgent.family)) {
                    browser = client.userAgent.family;
                    if (client.userAgent.major != null) {
                        browser += " " + client.userAgent.major;
                    }
                }

                if (client.os != null && client.os.family != null
                        && !"Other".equalsIgnoreCase(client.os.family)) {
                    os = client.os.family;
                    if (client.os.major != null) {
                        os += " " + client.os.major;
                    }
                }
            }

            if (secChUaPlatform != null && !secChUaPlatform.isBlank()) {
                String cleanPlatform = secChUaPlatform.replace("\"", "").trim();
                if (!cleanPlatform.isBlank()) {
                    os = cleanPlatform;
                }
            }

            if (browser != null && os != null) {
                return browser + " on " + os;
            } else if (browser != null) {
                return browser;
            } else if (os != null) {
                return os;
            }

            return (userAgent != null && userAgent.length() > 64)
                    ? userAgent.substring(0, 64)
                    : (userAgent != null ? userAgent : "Unknown");
        } catch (Exception e) {
            log.debug("Failed to parse device info: {}", e.getMessage());
            return "Unknown";
        }
    }
}
