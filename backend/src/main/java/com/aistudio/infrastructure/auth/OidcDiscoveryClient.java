package com.aistudio.infrastructure.auth;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OidcDiscoveryClient {

    private final RestClient restClient;
    private volatile OidcDiscoveryDocument cached;
    private volatile String cachedIssuer;

    public OidcDiscoveryClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                    {
                        setConnectTimeout(Duration.ofSeconds(10));
                        setReadTimeout(Duration.ofSeconds(15));
                    }
                })
                .build();
    }

    public OidcDiscoveryDocument discover(SsoProperties.Oidc oidc) {
        String issuer = normalizeIssuer(oidc.issuerUri());
        if (cached != null && cachedIssuer != null && cachedIssuer.equals(issuer)) {
            return cached;
        }
        String discoveryUrl = issuer + "/.well-known/openid-configuration";
        OidcDiscoveryDocument document = restClient.get()
                .uri(discoveryUrl)
                .retrieve()
                .body(OidcDiscoveryDocument.class);
        if (document == null
                || document.authorizationEndpoint() == null
                || document.tokenEndpoint() == null
                || document.userinfoEndpoint() == null) {
            throw new DomainException("CONFIG_ERROR", "OIDC discovery document is incomplete");
        }
        cached = document;
        cachedIssuer = issuer;
        return document;
    }

    private static String normalizeIssuer(String issuerUri) {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new DomainException("CONFIG_ERROR", "OIDC issuer URI is required");
        }
        return issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri.trim();
    }
}
