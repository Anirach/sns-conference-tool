package com.sns.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads (or mints) an {@code X-Request-Id} header, echoes it on the response, and binds it to the
 * SLF4J MDC so every structured log line for this request carries the correlation id. Upstream
 * services should propagate the header so the trace remains contiguous across hops.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String rid = req.getHeader(HEADER);
        if (rid == null || rid.isBlank() || rid.length() > 64) {
            rid = UUID.randomUUID().toString();
        }
        res.setHeader(HEADER, rid);
        MDC.put(MDC_KEY, rid);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
