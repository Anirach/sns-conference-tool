package com.sns.app.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.common.dto.Problem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limits the unauthenticated auth endpoints. Counting is delegated to the injected
 * {@link RateLimiter} ({@code InMemoryRateLimiter} by default, {@code RedissonRateLimiter} when
 * {@code sns.rate-limit.backend=redis}).
 *
 * <p>Buckets:
 * <ul>
 *   <li>{@code register_ip} — POST /api/auth/register, default 5 / hour / IP.</li>
 *   <li>{@code login_ip}    — POST /api/auth/login,    default 30 / hour / IP.</li>
 *   <li>{@code login_email} — POST /api/auth/login,    default 10 / hour / email
 *       (slows credential stuffing without the DoS surface that account-lockout creates).</li>
 *   <li>{@code refresh_ip}  — POST /api/auth/refresh,  default 60 / hour / IP.</li>
 * </ul>
 *
 * <p>The login filter peeks at the request body to extract the email; the body is then re-served
 * to downstream filters via {@link CachedBodyRequest}. We avoid Jackson tree parsing by using a
 * streaming {@link JsonParser} that stops at the first {@code "email"} field — typical request
 * bodies are < 200 bytes so the cost is negligible.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final ObjectMapper mapper;
    private final int registerPerIp;
    private final int loginPerIp;
    private final int loginPerEmail;
    private final int refreshPerIp;

    public RateLimitFilter(
        RateLimiter limiter,
        ObjectMapper mapper,
        @Value("${sns.rate-limit.register-per-ip-per-hour:5}") int registerPerIp,
        @Value("${sns.rate-limit.login-per-ip-per-hour:30}") int loginPerIp,
        @Value("${sns.rate-limit.login-per-email-per-hour:10}") int loginPerEmail,
        @Value("${sns.rate-limit.refresh-per-ip-per-hour:60}") int refreshPerIp
    ) {
        this.limiter = limiter;
        this.mapper = mapper;
        this.registerPerIp = registerPerIp;
        this.loginPerIp = loginPerIp;
        this.loginPerEmail = loginPerEmail;
        this.refreshPerIp = refreshPerIp;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }
        String path = req.getRequestURI();
        String ip = clientIp(req);
        switch (path) {
            case "/api/auth/register" -> {
                if (!limiter.tryAcquire("register_ip", ip, registerPerIp)) {
                    writeRateLimited(res, "Registration rate limit exceeded");
                    return;
                }
            }
            case "/api/auth/login" -> {
                if (!limiter.tryAcquire("login_ip", ip, loginPerIp)) {
                    writeRateLimited(res, "Login rate limit exceeded");
                    return;
                }
                CachedBodyRequest cached = new CachedBodyRequest(req);
                String email = peekEmail(cached.cachedBody());
                if (email != null && !limiter.tryAcquire("login_email", email.toLowerCase(), loginPerEmail)) {
                    writeRateLimited(res, "Login rate limit exceeded");
                    return;
                }
                chain.doFilter(cached, res);
                return;
            }
            case "/api/auth/refresh" -> {
                if (!limiter.tryAcquire("refresh_ip", ip, refreshPerIp)) {
                    writeRateLimited(res, "Refresh rate limit exceeded");
                    return;
                }
            }
            default -> { /* not throttled */ }
        }
        chain.doFilter(req, res);
    }

    private void writeRateLimited(HttpServletResponse res, String detail) throws IOException {
        res.setStatus(429);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Problem p = Problem.of(429, "Too many requests", detail);
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

    /** Streaming JSON peek for the {@code email} field. Returns null if absent or malformed. */
    private static String peekEmail(byte[] body) {
        if (body == null || body.length == 0) return null;
        try (JsonParser p = new JsonFactory().createParser(body)) {
            if (p.nextToken() != JsonToken.START_OBJECT) return null;
            while (p.nextToken() != null && p.currentToken() != JsonToken.END_OBJECT) {
                if ("email".equals(p.currentName())) {
                    p.nextToken();
                    return p.getValueAsString();
                }
                p.nextToken();
                p.skipChildren();
            }
        } catch (IOException ignored) {
            // Treat as no-email — handler will fail validation anyway.
        }
        return null;
    }

    /**
     * Re-readable request that lets us peek at the body for rate-limit keying without
     * draining the input stream that the controller needs to deserialise.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest delegate) throws IOException {
            super(delegate);
            this.body = delegate.getInputStream().readAllBytes();
        }

        byte[] cachedBody() { return body; }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            ByteArrayInputStream src = new ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override public int read() { return src.read(); }
                @Override public boolean isFinished() { return src.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener l) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
