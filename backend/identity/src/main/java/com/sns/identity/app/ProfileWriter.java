package com.sns.identity.app;

import java.util.UUID;

/**
 * Port for creating or updating a profile during registration. Implemented in the
 * profile module so identity doesn't have a reverse dependency.
 */
public interface ProfileWriter {
    void upsert(UUID userId, String firstName, String lastName, String academicTitle, String institution);
}
