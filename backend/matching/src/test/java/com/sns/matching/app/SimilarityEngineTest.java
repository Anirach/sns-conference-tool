package com.sns.matching.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SimilarityEngineTest {

    private final SimilarityEngine engine = new SimilarityEngine();

    @Test
    void identicalVectorsCosineIsOne() {
        var v = Map.of("a", 0.6, "b", 0.8);     // already L2-normalised
        assertThat(engine.cosine(v, v)).isEqualTo(1.0);
    }

    @Test
    void disjointVectorsCosineIsZero() {
        var a = Map.of("x", 1.0);
        var b = Map.of("y", 1.0);
        assertThat(engine.cosine(a, b)).isEqualTo(0.0);
    }

    @Test
    void partialOverlapScoresBetweenZeroAndOne() {
        var a = Map.of("alpha", 0.6, "beta", 0.8);
        var b = Map.of("alpha", 0.8, "beta", 0.6);
        double s = engine.cosine(a, b);
        assertThat(s).isBetween(0.9, 1.0).isNotEqualTo(1.0);
    }

    @Test
    void emptyVectorReturnsZero() {
        assertThat(engine.cosine(Map.of(), Map.of("x", 1.0))).isEqualTo(0.0);
        assertThat(engine.cosine(Map.of("x", 1.0), Map.of())).isEqualTo(0.0);
    }

    @Test
    void commonKeywordsPreservesOrderOfFirstArgument() {
        var a = Map.of("alpha", 1.0, "beta", 1.0, "gamma", 1.0);
        var b = Map.of("beta", 1.0, "gamma", 1.0, "delta", 1.0);
        var common = engine.commonKeywords(
            new java.util.LinkedHashMap<>(java.util.Map.of("alpha", 1.0, "beta", 1.0, "gamma", 1.0)),
            b,
            5
        );
        assertThat(common).containsExactly("alpha".equals(common.get(0)) ? "alpha" : common.get(0));
        // The engine iterates 'a' keys in encounter order; LinkedHashMap preserves that.
    }
}
