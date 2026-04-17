package com.sns.interest.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts a top-K keyword list + L2-normalised TF(-IDF) weight vector from free text.
 * <p>
 * Two implementations ship:
 * <ul>
 *   <li>{@link TfKeywordExtractor} — default; dependency-free TF + stopword list.</li>
 *   <li>{@link OpenNlpKeywordExtractor} — activated when {@code sns.nlp.models-dir} points at a
 *       directory containing OpenNLP {@code en-token.bin}, {@code en-pos-maxent.bin}, and
 *       {@code en-lemmatizer.bin}. RAKE-inspired candidate phrase extraction, lemmatised
 *       terms, higher recall on noun-phrase heavy text.</li>
 * </ul>
 * Both produce the same {@link Extraction} shape so callers can swap transparently.
 */
public interface KeywordExtractor {

    Extraction extract(String text);

    record Extraction(List<String> keywords, Map<String, Double> weights) {
        public static final Extraction EMPTY = new Extraction(List.of(), Map.of());

        public String[] keywordsArray() { return keywords.toArray(new String[0]); }

        public Map<String, Double> vector() { return weights; }

        public Set<String> keywordSet() { return new HashSet<>(keywords); }

        public static List<String> intersect(Extraction a, Extraction b) {
            Set<String> bs = b.keywordSet();
            return a.keywords.stream().filter(bs::contains).toList();
        }

        public static List<String> intersectByName(List<String> a, List<String> b) {
            Set<String> bs = new HashSet<>(b);
            return Arrays.asList(a.stream().filter(bs::contains).toArray(String[]::new));
        }
    }
}
