package com.aistudio.infrastructure.ratelimit;

import com.aistudio.infrastructure.config.AiProperties;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AiRateLimitFilter extends OncePerRequestFilter {

    private final int aiPerMinute;
    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public AiRateLimitFilter(AiProperties properties, ObjectMapper objectMapper) {
        this.aiPerMinute = properties.rateLimit() == null || properties.rateLimit().aiPerMinute() <= 0
                ? 30
                : properties.rateLimit().aiPerMinute();
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean aiAction = path.contains("/ai/");
        boolean chatMessage = path.contains("/conversations/") && path.endsWith("/messages");
        return !(aiAction || chatMessage);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = resolveKey(request);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000L;
        Deque<Long> deque = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < windowStart) {
                deque.removeFirst();
            }
            if (deque.size() >= aiPerMinute) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", "60");
                response.setHeader("X-RateLimit-Limit", String.valueOf(aiPerMinute));
                response.setHeader("X-RateLimit-Remaining", "0");
                objectMapper.writeValue(response.getWriter(), Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 429,
                        "code", "RATE_LIMITED",
                        "message", "AI rate limit exceeded. Try again shortly.",
                        "path", request.getRequestURI()
                ));
                return;
            }
            deque.addLast(now);
            response.setHeader("X-RateLimit-Limit", String.valueOf(aiPerMinute));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, aiPerMinute - deque.size())));
        }
        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return "user:" + user.getId();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
