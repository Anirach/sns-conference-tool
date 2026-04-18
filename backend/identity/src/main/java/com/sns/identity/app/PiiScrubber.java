package com.sns.identity.app;

import java.util.regex.Pattern;

/**
 * Shared PII redaction for audit payloads and log messages. Masks email addresses to
 * {@code a***@domain} and redacts anything that looks like a JWT or access token.
 */
public final class PiiScrubber {

    private PiiScrubber() {}

    private static final Pattern EMAIL = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b"
    );
    // JWT-shaped: three base64url segments where each segment is at least 8 chars and the
    // whole string is at least ~80 chars. Stops short of matching Java class FQNs like
    // `com.sns.app.config.RateLimitFilter` which would otherwise be redacted as JWTs.
    private static final Pattern JWT = Pattern.compile(
        "\\b[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}\\b"
    );
    private static final Pattern BEARER = Pattern.compile(
        "(?i)(bearer\\s+)[A-Za-z0-9._-]+"
    );
    private static final Pattern LATLON = Pattern.compile(
        "\\b(-?\\d+\\.\\d{3,})\\b"
    );

    public static String mask(String s) {
        if (s == null || s.isEmpty()) return s;
        String out = EMAIL.matcher(s).replaceAll(m -> {
            String local = m.group();
            int at = local.indexOf('@');
            String userPart = local.substring(0, at);
            String domain = local.substring(at + 1);
            String head = userPart.isEmpty() ? "" : userPart.substring(0, 1);
            return head + "***@" + domain;
        });
        out = BEARER.matcher(out).replaceAll("$1***");
        out = JWT.matcher(out).replaceAll("***");
        out = LATLON.matcher(out).replaceAll(m -> {
            String v = m.group(1);
            // Keep 2 decimals — coarse enough to anonymise, precise enough to debug region.
            int dot = v.indexOf('.');
            return v.substring(0, Math.min(v.length(), dot + 3));
        });
        return out;
    }
}
