package com.sns.matching.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Cosine similarity over TF-IDF keyword vectors. Inputs are L2-normalised already (see
 * {@code KeywordExtractor}), so cosine reduces to the dot product of overlapping dimensions.
 * The similarity score is clipped to [0, 1].
 */
@Service
public class SimilarityEngine {

    public double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0.0;
        for (var e : small.entrySet()) {
            Double v = large.get(e.getKey());
            if (v != null) dot += e.getValue() * v;
        }
        if (dot < 0) return 0.0;
        if (dot > 1) return 1.0;
        return dot;
    }

    public List<String> commonKeywords(Map<String, Double> a, Map<String, Double> b, int limit) {
        Set<String> bs = new HashSet<>(b.keySet());
        List<String> common = new ArrayList<>();
        for (String k : a.keySet()) {
            if (bs.contains(k)) {
                common.add(k);
                if (common.size() == limit) break;
            }
        }
        return common;
    }
}
