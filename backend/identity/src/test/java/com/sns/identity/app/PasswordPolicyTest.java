package com.sns.identity.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> policy.validate("ada@example.com", "short"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsPasswordEqualToEmailLocalPart() {
        assertThatThrownBy(() -> policy.validate("ada.lovelace@example.com", "ada.lovelace"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsCommonBlocklistEntries() {
        assertThatThrownBy(() -> policy.validate("alice@example.com", "password1"))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> policy.validate("alice@example.com", "letmein1"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("alice@example.com", "C0rrectHorseBatteryStaple"))
            .doesNotThrowAnyException();
    }

    @Test
    void caseInsensitiveBlocklist() {
        assertThatThrownBy(() -> policy.validate("alice@example.com", "PASSWORD1"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void caseInsensitiveEmailMatch() {
        assertThatThrownBy(() -> policy.validate("Ada.Lovelace@example.com", "ADA.LOVELACE"))
            .isInstanceOf(ResponseStatusException.class);
    }
}
