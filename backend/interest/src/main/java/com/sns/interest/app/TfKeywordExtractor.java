package com.sns.interest.app;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Dependency-free TF extractor. Tokenises text, strips stopwords and short tokens, and returns a
 * map from keyword → term-frequency weight (L2-normalised). Default implementation; replaced by
 * {@link OpenNlpKeywordExtractor} when OpenNLP models are configured.
 */
@Service
public class TfKeywordExtractor implements KeywordExtractor {

    private static final int TOP_K = 25;

    private static final Set<String> STOPWORDS = Set.of(
        "a","an","and","are","as","at","be","by","for","from","has","have","he","in","is","it","its",
        "of","on","or","that","the","to","was","were","will","with","this","these","those","which",
        "but","not","they","them","their","there","here","we","you","your","our","us","about","into",
        "over","under","between","among","after","before","such","also","than","then","so","if","while",
        "can","could","may","might","should","would","do","does","did","been","being","because","each",
        "any","every","all","some","more","most","other","same","no","nor","own","per","via","etc"
    );

    @Override
    public Extraction extract(String text) {
        if (text == null) return Extraction.EMPTY;

        String[] rawTokens = text.toLowerCase()
            .replaceAll("[^\\p{L}\\p{N}\\s-]", " ")
            .split("\\s+");

        Map<String, Integer> counts = new HashMap<>();
        for (String t : rawTokens) {
            if (t.isBlank()) continue;
            String tok = t.replaceAll("^-+|-+$", "");
            if (tok.length() < 3) continue;
            if (STOPWORDS.contains(tok)) continue;
            if (tok.chars().allMatch(Character::isDigit)) continue;
            counts.merge(tok, 1, Integer::sum);
        }

        if (counts.isEmpty()) return Extraction.EMPTY;

        List<Map.Entry<String, Integer>> ordered = new java.util.ArrayList<>(counts.entrySet());
        ordered.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
            .thenComparing(Map.Entry::getKey));

        List<Map.Entry<String, Integer>> top = ordered.subList(0, Math.min(TOP_K, ordered.size()));

        double l2 = 0.0;
        for (var e : top) l2 += (double) e.getValue() * e.getValue();
        l2 = Math.sqrt(l2);
        double norm = l2 == 0.0 ? 1.0 : l2;

        Map<String, Double> vector = new LinkedHashMap<>();
        for (var e : top) vector.put(e.getKey(), e.getValue() / norm);

        return new Extraction(vector.keySet().stream().toList(), vector);
    }
}
