package com.sns.identity.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiScrubberTest {

    @Test
    void masksEmailPreservingDomain() {
        assertThat(PiiScrubber.mask("reach out to alice@example.com today"))
            .contains("a***@example.com")
            .doesNotContain("alice@example.com");
    }

    @Test
    void masksBearerToken() {
        assertThat(PiiScrubber.mask("Authorization: Bearer abc.def.ghi"))
            .contains("Bearer ***")
            .doesNotContain("abc.def.ghi");
    }

    @Test
    void masksBareJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb28ifQ.signatureblob";
        assertThat(PiiScrubber.mask("token=" + jwt)).doesNotContain(jwt);
    }

    @Test
    void truncatesHighPrecisionLatLon() {
        String masked = PiiScrubber.mask("pos 13.736543210 / 100.552291133");
        assertThat(masked).contains("13.73").contains("100.55");
        assertThat(masked).doesNotContain("13.736543210");
    }

    @Test
    void emptyAndNullAreReturnedUnchanged() {
        assertThat(PiiScrubber.mask(null)).isNull();
        assertThat(PiiScrubber.mask("")).isEmpty();
    }
}
