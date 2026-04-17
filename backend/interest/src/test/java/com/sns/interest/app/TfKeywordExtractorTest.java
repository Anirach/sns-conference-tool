package com.sns.interest.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TfKeywordExtractorTest {

    private final TfKeywordExtractor extractor = new TfKeywordExtractor();

    @Test
    void emptyInputReturnsEmptyExtraction() {
        assertThat(extractor.extract(null).keywords()).isEmpty();
        assertThat(extractor.extract("").keywords()).isEmpty();
        assertThat(extractor.extract("   ").keywords()).isEmpty();
    }

    @Test
    void stopwordsAndShortTokensAreDropped() {
        var e = extractor.extract("The cat sat on a mat");
        assertThat(e.keywords()).containsExactlyInAnyOrder("cat", "sat", "mat");
    }

    @Test
    void topKByFrequencyWithAlphabeticalTieBreak() {
        var text = "alpha alpha beta beta gamma";
        var e = extractor.extract(text);
        // alpha and beta tie at 2; gamma at 1. Alphabetical ties keep alpha first.
        assertThat(e.keywords()).startsWith("alpha", "beta", "gamma");
    }

    @Test
    void vectorIsL2Normalised() {
        var e = extractor.extract("foo foo bar baz baz baz");
        double sumSquares = e.weights().values().stream()
            .mapToDouble(d -> d * d).sum();
        assertThat(sumSquares).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void hyphenatedTokensArePreserved() {
        var e = extractor.extract("graph-neural-networks are powerful networks and");
        assertThat(e.keywords()).contains("graph-neural-networks");
    }
}
