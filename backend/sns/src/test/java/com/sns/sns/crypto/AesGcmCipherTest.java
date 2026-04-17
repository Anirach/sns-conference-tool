package com.sns.sns.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AesGcmCipherTest {

    private final AesGcmCipher cipher = new AesGcmCipher("test-master-key-do-not-use-in-prod");

    @Test
    void encryptThenDecryptRoundTripsUtf8() {
        var enc = cipher.encryptString("s3cret-token-🔒");
        assertThat(enc.iv()).hasSize(12);
        assertThat(enc.ciphertext()).isNotEmpty();
        assertThat(cipher.decryptToString(enc.iv(), enc.ciphertext())).isEqualTo("s3cret-token-🔒");
    }

    @Test
    void eachEncryptionUsesAFreshIv() {
        var a = cipher.encryptString("same");
        var b = cipher.encryptString("same");
        assertThat(a.iv()).isNotEqualTo(b.iv());
        assertThat(a.ciphertext()).isNotEqualTo(b.ciphertext());
    }

    @Test
    void tamperedCiphertextFailsAuthTag() {
        var enc = cipher.encryptString("authenticated-payload");
        byte[] tampered = enc.ciphertext().clone();
        tampered[0] ^= 0x01;
        assertThatThrownBy(() -> cipher.decrypt(enc.iv(), tampered))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyFailsAuthTag() {
        var other = new AesGcmCipher("a-different-master-key");
        var enc = cipher.encryptString("secret");
        assertThatThrownBy(() -> other.decrypt(enc.iv(), enc.ciphertext()))
            .isInstanceOf(IllegalStateException.class);
    }
}
