package com.vaultix.gateway.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vaultix.gateway.service.DeviceParserService;
import com.vaultix.gateway.service.GeoIpService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class ClientContextFilter extends OncePerRequestFilter {

    private final GeoIpService geoIpService;
    private final DeviceParserService deviceParserService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveRealClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String secChUaPlatform = request.getHeader("Sec-CH-UA-Platform");

        String deviceInfo = deviceParserService.resolveDeviceInfo(userAgent, secChUaPlatform);
        String location = geoIpService.resolveLocation(clientIp);

        log.debug("Enriching request: IP={}, Device={}, Location={}", clientIp, deviceInfo, location);

        ClientContextRequestWrapper wrappedRequest = new ClientContextRequestWrapper(
                request,
                clientIp,
                deviceInfo,
                location
        );

        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * Extracts the real client IP from edge headers (e.g. Cloudflare, reverse
     * proxy) or falls back to remote address.
     */
    private String resolveRealClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            int commaIndex = xForwardedFor.indexOf(',');
            return (commaIndex != -1) ? xForwardedFor.substring(0, commaIndex).trim() : xForwardedFor.trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
