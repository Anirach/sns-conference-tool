package com.sns.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Baseline response headers for every HTTP response. CSP is deliberately tight — the web app is
 * same-origin and doesn't load third-party scripts. When embedded in the Flutter WebView, the
 * headers are a no-op (native WebView ignores CSP for local content) but stay in force for any
 * browser fallback.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "same-origin");
        res.setHeader("Permissions-Policy", "camera=(self), microphone=(), geolocation=(self)");
        res.setHeader("Content-Security-Policy",
            "default-src 'self'; "
            + "img-src 'self' data: https:; "
            + "style-src 'self' 'unsafe-inline'; "
            + "script-src 'self'; "
            + "connect-src 'self' ws: wss:; "
            + "frame-ancestors 'self'; "
            + "object-src 'none'; "
            + "base-uri 'self'");
        // Auth responses (tokens, verification cookies, etc.) must never sit in any cache.
        // Misconfigured CDNs / shared proxies have leaked bearer tokens this way before.
        if (req.getRequestURI() != null && req.getRequestURI().startsWith("/api/auth/")) {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            res.setHeader("Pragma", "no-cache");
        }
        chain.doFilter(req, res);
    }
}
