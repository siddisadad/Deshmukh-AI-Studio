package com.aistudio.infrastructure.security;

import com.aistudio.infrastructure.persistence.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String metricsScrapeToken;
    private final String billingUsageSyncToken;

    public JwtAuthFilter(
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${aistudio.metrics.scrape-token:}") String metricsScrapeToken,
            @Value("${aistudio.billing.usage-sync-token:}") String billingUsageSyncToken
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.metricsScrapeToken = metricsScrapeToken;
        this.billingUsageSyncToken = billingUsageSyncToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (authenticateMetricsScrape(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (authenticateBillingUsageSync(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    userRepository.findById(parsed.userId()).ifPresent(user -> {
                        AuthenticatedUser principal = new AuthenticatedUser(
                                user.getId(), user.getEmail(), user.getPasswordHash());
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean authenticateMetricsScrape(HttpServletRequest request) {
        if (metricsScrapeToken == null || metricsScrapeToken.isBlank()) {
            return false;
        }
        if (!request.getRequestURI().endsWith("/actuator/prometheus")) {
            return false;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        String token = header.substring(7);
        if (!token.equals(metricsScrapeToken)) {
            return false;
        }
        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "metrics@internal",
                null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return true;
    }

    private boolean authenticateBillingUsageSync(HttpServletRequest request) {
        if (billingUsageSyncToken == null || billingUsageSyncToken.isBlank()) {
            return false;
        }
        if (!request.getRequestURI().endsWith("/api/v1/billing/stripe/sync-metered-usage")) {
            return false;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        String token = header.substring(7);
        if (!token.equals(billingUsageSyncToken)) {
            return false;
        }
        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "billing-sync@internal",
                null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return true;
    }
}
