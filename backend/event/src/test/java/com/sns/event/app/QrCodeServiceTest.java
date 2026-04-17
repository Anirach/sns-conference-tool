package com.sns.event.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    private final QrCodeService qr = new QrCodeService("unit-test-hmac-key");

    @Test
    void hashIsCaseAndWhitespaceInsensitive() {
        assertThat(qr.hash("NEURIPS2026")).isEqualTo(qr.hash("  neurips2026 "));
    }

    @Test
    void signedTokenRoundTrips() {
        String tok = qr.issue("NEURIPS2026", Instant.now().plusSeconds(60));
        assertThat(qr.verify(tok)).isEqualTo("NEURIPS2026");
    }

    @Test
    void expiredTokenRejected() {
        String tok = qr.issue("NEURIPS2026", Instant.now().minusSeconds(5));
        assertThatThrownBy(() -> qr.verify(tok))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void tamperedSignatureRejected() {
        String tok = qr.issue("NEURIPS2026", Instant.now().plusSeconds(60));
        int dot = tok.indexOf('.');
        String tampered = tok.substring(0, dot + 1) + "AAAA" + tok.substring(dot + 1);
        assertThatThrownBy(() -> qr.verify(tampered))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void differentKeyGeneratesDifferentSignature() {
        var a = new QrCodeService("key-one");
        var b = new QrCodeService("key-two");
        String token = a.issue("ABC", Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> b.verify(token))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryVerifyReturnsNullOnMalformed() {
        assertThat(qr.tryVerify("not-a-token")).isNull();
        assertThat(qr.tryVerify("foo.bar")).isNull();
    }
}
