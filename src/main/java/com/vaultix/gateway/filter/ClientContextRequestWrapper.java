package com.vaultix.gateway.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vaultix.common.constant.CommonHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class ClientContextRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, List<String>> headers = new LinkedHashMap<>();

    public ClientContextRequestWrapper(
            HttpServletRequest request,
            String clientIp,
            String deviceInfo,
            String deviceLocation) {
        super(request);

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!CommonHeaders.X_CLIENT_IP.equalsIgnoreCase(name)
                        && !CommonHeaders.X_DEVICE_INFO.equalsIgnoreCase(name)
                        && !CommonHeaders.X_DEVICE_LOCATION.equalsIgnoreCase(name)) {
                    headers.put(name, Collections.list(request.getHeaders(name)));
                }
            }
        }

        if (clientIp != null && !clientIp.isBlank()) {
            headers.put(CommonHeaders.X_CLIENT_IP, List.of(clientIp));
        }
        if (deviceInfo != null && !deviceInfo.isBlank()) {
            headers.put(CommonHeaders.X_DEVICE_INFO, List.of(deviceInfo));
        }
        if (deviceLocation != null && !deviceLocation.isBlank()) {
            headers.put(CommonHeaders.X_DEVICE_LOCATION, List.of(deviceLocation));
        }
    }

    @Override
    public String getHeader(String name) {
        List<String> values = findHeaderValues(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = findHeaderValues(name);
        return Collections.enumeration(values != null ? values : Collections.emptyList());
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    private List<String> findHeaderValues(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name);
        if (values != null) {
            return values;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
