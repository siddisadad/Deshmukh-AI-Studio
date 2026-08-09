package com.aistudio.infrastructure.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class OrgSloRequestFilter extends OncePerRequestFilter {

    private static final Pattern ORG_PATH = Pattern.compile("/organizations/([0-9a-fA-F\\-]{36})");
    private static final Pattern PROJECT_PATH = Pattern.compile("/projects/([0-9a-fA-F\\-]{36})");

    private final OrgSloProjectResolver projectResolver;

    public OrgSloRequestFilter(OrgSloProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            resolveOrganizationId(request).ifPresent(orgId -> OrgSloContext.setOrganizationId(orgId.toString()));
            filterChain.doFilter(request, response);
        } finally {
            OrgSloContext.clear();
        }
    }

    private Optional<UUID> resolveOrganizationId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Matcher orgMatcher = ORG_PATH.matcher(uri);
        if (orgMatcher.find()) {
            return parseUuid(orgMatcher.group(1));
        }
        Matcher projectMatcher = PROJECT_PATH.matcher(uri);
        if (projectMatcher.find()) {
            return parseUuid(projectMatcher.group(1)).flatMap(projectResolver::resolveOrganizationId);
        }
        return Optional.empty();
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
