package com.sns.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.common.dto.Problem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP rate limit on the unauthenticated {@code /api/auth/register} endpoint.
 * <p>
 * Fixed-window implementation backed by an in-memory map keyed by IP. Good enough for a single-pod
 * deployment — will be swapped for a Redis-backed bucket (Redisson) in Phase 4 when we have multi-pod
 * horizontal scale.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxPerHour;
    private final ObjectMapper mapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
        @Value("${sns.rate-limit.register-per-ip-per-hour:5}") int maxPerHour,
        ObjectMapper mapper
    ) {
        this.maxPerHour = maxPerHour;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(req.getMethod()) && "/api/auth/register".equals(req.getRequestURI())) {
            String ip = clientIp(req);
            Window w = windows.compute(ip, (k, existing) -> {
                Instant now = Instant.now();
                if (existing == null || existing.start.isBefore(now.minus(Duration.ofHours(1)))) {
                    return new Window(now, 1);
                }
                return new Window(existing.start, existing.count + 1);
            });
            if (w.count > maxPerHour) {
                writeRateLimited(res);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void writeRateLimited(HttpServletResponse res) throws IOException {
        res.setStatus(429);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Problem p = Problem.of(429, "Too many requests", "Registration rate limit exceeded");
        res.getWriter().write(mapper.writeValueAsString(p));
    }

    private static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        return req.getRemoteAddr();
    }

    private record Window(Instant start, int count) {}
}
