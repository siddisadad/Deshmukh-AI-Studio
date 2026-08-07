package com.aistudio.infrastructure.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionCorsValidator implements ApplicationRunner {

    private final CorsProperties corsProperties;

    public ProductionCorsValidator(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> origins = corsProperties.allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException(
                    "Production requires CORS_ORIGINS with at least one HTTPS app origin");
        }
        for (String origin : origins) {
            validateOrigin(origin);
        }
    }

    private static void validateOrigin(String origin) {
        final URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Production CORS origin is not a valid URI: " + origin, ex);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "Production CORS origins must be absolute HTTPS URIs with a host: " + origin);
        }

        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(
                    "Production CORS origins must use HTTPS and must not include localhost: " + origin);
        }

        if (isLoopbackHost(host)) {
            throw new IllegalStateException(
                    "Production CORS origins must use HTTPS and must not include localhost: " + origin);
        }
    }

    private static boolean isLoopbackHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if ("localhost".equals(normalized)) {
            return true;
        }
        if ("::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized)) {
            return true;
        }
        return isIpv4Loopback(normalized);
    }

    /** True for any IPv4 address in 127.0.0.0/8. */
    private static boolean isIpv4Loopback(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            if (first != 127) {
                return false;
            }
            for (int i = 1; i < 4; i++) {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
