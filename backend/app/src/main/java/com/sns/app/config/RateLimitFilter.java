package com.sns.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.common.dto.Problem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP rate limit on the unauthenticated {@code /api/auth/register} endpoint. Counting is
 * delegated to the injected {@link RateLimiter} — {@code InMemoryRateLimiter} by default,
 * {@code RedissonRateLimiter} when {@code sns.rate-limit.backend=redis}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final ObjectMapper mapper;

    public RateLimitFilter(RateLimiter limiter, ObjectMapper mapper) {
        this.limiter = limiter;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(req.getMethod()) && "/api/auth/register".equals(req.getRequestURI())) {
            String ip = clientIp(req);
            if (!limiter.tryAcquire(ip)) {
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
}
