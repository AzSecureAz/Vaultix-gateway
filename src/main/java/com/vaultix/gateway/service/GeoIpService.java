package com.vaultix.gateway.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.vaultix.gateway.config.properties.GeoIpProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeoIpService implements AutoCloseable {

    private final DatabaseReader databaseReader;

    private final Cache<String, String> locationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    public GeoIpService(GeoIpProperties geoIpProperties) {
        Path configuredPath = Path.of(geoIpProperties.databasePath());
        DatabaseReader reader = null;

        if (Files.isRegularFile(configuredPath)) {
            try {
                File databaseFile = configuredPath.toFile();
                reader = new DatabaseReader.Builder(databaseFile)
                        .withCache(new CHMCache())
                        .build();
                log.info("GeoLite2-City database loaded successfully with CHMCache from: {}", configuredPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to initialize GeoIP DatabaseReader from configured path: {}", configuredPath, e);
            }
        } else {
            Path fallbackPath = Paths.get("..", configuredPath.toString());
            if (Files.isRegularFile(fallbackPath)) {
                try {
                    File databaseFile = fallbackPath.toFile();
                    reader = new DatabaseReader.Builder(databaseFile)
                            .withCache(new CHMCache())
                            .build();
                    log.info("GeoLite2-City database loaded successfully with CHMCache from fallback path: {}", fallbackPath.toAbsolutePath());
                } catch (IOException e) {
                    log.error("Failed to initialize GeoIP DatabaseReader from fallback path: {}", fallbackPath, e);
                }
            } else {
                log.warn("GeoLite2-City database not found at '{}'. GeoIP resolution will return 'Unknown'. "
                        + "Place GeoLite2-City.mmdb into the configured path to enable geolocation.", configuredPath);
            }
        }

        this.databaseReader = reader;
    }

    /**
     * Resolves an IP address into a comprehensive geolocation string including
     * city, postal/pincode, region/state (with ISO code), country (with ISO
     * code), coordinates (latitude, longitude), accuracy radius, and timezone.
     *
     * <p>
     * Examples:
     * <ul>
     * <li>Full resolution:
     * {@code "Mumbai, 400001, Maharashtra (MH), India (IN) (Lat: 19.0760, Lon: 72.8777, Accuracy: 50km, Timezone: Asia/Kolkata)"}</li>
     * <li>Without postal:
     * {@code "Mumbai, Maharashtra (MH), India (IN) (Lat: 19.0760, Lon: 72.8777, Accuracy: 50km, Timezone: Asia/Kolkata)"}</li>
     * <li>Country-only:
     * {@code "India (IN) (Lat: 20.0000, Lon: 77.0000, Accuracy: 100km, Timezone: Asia/Kolkata)"}</li>
     * <li>Private / Loopback IP: {@code "Local"}</li>
     * <li>Unresolvable: {@code "Unknown"}</li>
     * </ul>
     *
     * @param ip the client IP address
     * @return the resolved comprehensive location, or 'Local', or 'Unknown'
     */
    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return "Unknown";
        }

        return locationCache.get(ip.strip(), this::lookupLocation);
    }

    private String lookupLocation(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);

            if (inetAddress.isLoopbackAddress() || inetAddress.isSiteLocalAddress()
                    || inetAddress.isLinkLocalAddress() || inetAddress.isAnyLocalAddress()) {
                return "Local";
            }

            if (databaseReader == null) {
                log.warn("GeoIP DatabaseReader is not initialized (database file missing or not loaded). Returning 'Unknown' for IP: {}", ip);
                return "Unknown";
            }

            CityResponse response = databaseReader.city(inetAddress);
            String city = response.city() != null ? response.city().name() : null;
            String postal = response.postal() != null ? response.postal().code() : null;
            String subdivision = response.mostSpecificSubdivision() != null ? response.mostSpecificSubdivision().name() : null;
            String subdivisionCode = response.mostSpecificSubdivision() != null ? response.mostSpecificSubdivision().isoCode() : null;
            String country = response.country() != null ? response.country().name() : null;
            String countryCode = response.country() != null ? response.country().isoCode() : null;

            StringJoiner locationJoiner = new StringJoiner(", ");
            if (city != null && !city.isBlank()) {
                locationJoiner.add(city);
            }
            if (postal != null && !postal.isBlank()) {
                locationJoiner.add(postal);
            }
            if (subdivision != null && !subdivision.isBlank() && !subdivision.equalsIgnoreCase(city)) {
                String subPart = (subdivisionCode != null && !subdivisionCode.isBlank())
                        ? subdivision + " (" + subdivisionCode + ")"
                        : subdivision;
                locationJoiner.add(subPart);
            }
            if (country != null && !country.isBlank()) {
                String countryPart = (countryCode != null && !countryCode.isBlank())
                        ? country + " (" + countryCode + ")"
                        : country;
                locationJoiner.add(countryPart);
            } else if (countryCode != null) {
                locationJoiner.add(countryCode);
            }

            StringBuilder fullLocation = new StringBuilder(locationJoiner.toString());

            if (response.location() != null) {
                Double lat = response.location().latitude();
                Double lon = response.location().longitude();
                Integer accuracy = response.location().accuracyRadius();
                String timeZone = response.location().timeZone();

                if (lat != null && lon != null) {
                    if (!fullLocation.isEmpty()) {
                        fullLocation.append(" ");
                    }
                    fullLocation.append(String.format(java.util.Locale.ROOT, "(Lat: %.4f, Lon: %.4f", lat, lon));
                    if (accuracy != null) {
                        fullLocation.append(", Accuracy: ").append(accuracy).append("km");
                    }
                    if (timeZone != null && !timeZone.isBlank()) {
                        fullLocation.append(", Timezone: ").append(timeZone);
                    }
                    fullLocation.append(")");
                } else if (timeZone != null && !timeZone.isBlank()) {
                    if (!fullLocation.isEmpty()) {
                        fullLocation.append(" ");
                    }
                    fullLocation.append("(Timezone: ").append(timeZone).append(")");
                }
            }

            String result = fullLocation.toString().strip();
            return !result.isBlank() ? result : "Unknown";
        } catch (AddressNotFoundException e) {
            log.debug("IP address '{}' not found in GeoIP database", ip);
            return "Unknown";
        } catch (IOException | GeoIp2Exception e) {
            log.debug("Failed to resolve GeoIP location for IP '{}': {}", ip, e.getMessage());
            return "Unknown";
        }
    }

    @Override
    public void close() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
                log.info("GeoIP DatabaseReader closed");
            } catch (IOException e) {
                log.warn("Error closing GeoIP DatabaseReader", e);
            }
        }
    }
}
