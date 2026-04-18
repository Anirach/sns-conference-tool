package com.sns.identity.domain;

/**
 * Coarse role attached to {@link UserEntity}. The participant API treats every role identically;
 * the {@code /api/admin/**} chain accepts {@link #ADMIN} or {@link #SUPER_ADMIN}. Only
 * {@link #SUPER_ADMIN} can promote / demote roles or hard-delete users (enforced in
 * {@code AdminUserService}, not by the URL matcher).
 *
 * <p>Persisted as a {@code TEXT} column with a CHECK constraint (Flyway V10).
 */
public enum Role {
    USER,
    ORGANIZER,
    ADMIN,
    SUPER_ADMIN
}
