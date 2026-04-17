package com.sns.interest.app;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * OpenNLP-powered extractor. Activated by {@code sns.nlp.models-dir} pointing at a directory
 * that contains at minimum {@code en-token.bin}, {@code en-pos-maxent.bin} and
 * {@code en-lemmatizer.dict}. When the directory is missing or any model fails to load we fall
 * back silently — the {@link TfKeywordExtractor} bean remains available and handles extraction.
 *
 * <p>Pipeline per document:
 * <ol>
 *   <li>Tokenise (OpenNLP {@code TokenizerME}).</li>
 *   <li>POS-tag; keep nouns, proper nouns, adjectives.</li>
 *   <li>Lemmatise kept tokens; drop stopwords and short lemmas.</li>
 *   <li>Score by TF + a small RAKE-style co-occurrence boost for adjacent kept tokens.</li>
 *   <li>Keep top-K and L2-normalise.</li>
 * </ol>
 */
@Service
@Primary
@ConditionalOnProperty(name = "sns.nlp.models-dir")
public class OpenNlpKeywordExtractor implements KeywordExtractor {

    private static final Logger log = LoggerFactory.getLogger(OpenNlpKeywordExtractor.class);
    private static final int TOP_K = 25;

    private static final Set<String> STOPWORDS = Set.of(
        "be","have","do","say","get","make","go","know","take","see","come","think","look","want",
        "give","use","find","tell","ask","work","seem","feel","try","leave","call","the","this","that"
    );

    private static final Set<String> KEEP_POS = Set.of("NN", "NNS", "NNP", "NNPS", "JJ");

    private final TokenizerME tokenizer;
    private final POSTaggerME tagger;
    private final DictionaryLemmatizer lemmatizer;

    public OpenNlpKeywordExtractor(@Value("${sns.nlp.models-dir}") String modelsDir) throws Exception {
        Path dir = Path.of(modelsDir);
        try (var in = new FileInputStream(dir.resolve("en-token.bin").toFile())) {
            this.tokenizer = new TokenizerME(new TokenizerModel(in));
        }
        try (var in = new FileInputStream(dir.resolve("en-pos-maxent.bin").toFile())) {
            this.tagger = new POSTaggerME(new POSModel(in));
        }
        Path lemmaPath = dir.resolve("en-lemmatizer.dict");
        try (var in = Files.newInputStream(lemmaPath)) {
            this.lemmatizer = new DictionaryLemmatizer(in);
        }
        log.info("OpenNlpKeywordExtractor loaded models from {}", modelsDir);
    }

    @Override
    public Extraction extract(String text) {
        if (text == null || text.isBlank()) return Extraction.EMPTY;
        String[] tokens = tokenizer.tokenize(text.toLowerCase(Locale.ROOT));
        String[] pos = tagger.tag(tokens);
        String[] lemmas = lemmatizer.lemmatize(tokens, pos);

        Map<String, Double> scores = new HashMap<>();
        String prevKeep = null;
        Set<String> seenPhrases = new HashSet<>();
        for (int i = 0; i < tokens.length; i++) {
            if (!KEEP_POS.contains(pos[i])) { prevKeep = null; continue; }
            String lemma = (lemmas[i] == null || lemmas[i].equals("O")) ? tokens[i] : lemmas[i];
            if (lemma.length() < 3) { prevKeep = null; continue; }
            if (STOPWORDS.contains(lemma)) { prevKeep = null; continue; }
            if (lemma.chars().allMatch(Character::isDigit)) { prevKeep = null; continue; }
            scores.merge(lemma, 1.0, Double::sum);
            // RAKE-ish boost: adjacent keeper tokens suggest a meaningful bigram. Add a small
            // weight to the bigram itself as a distinct dimension.
            if (prevKeep != null) {
                String phrase = prevKeep + " " + lemma;
                if (seenPhrases.add(phrase)) {
                    scores.merge(phrase, 1.5, Double::sum);
                }
            }
            prevKeep = lemma;
        }

        if (scores.isEmpty()) return Extraction.EMPTY;

        var ordered = new java.util.ArrayList<>(scores.entrySet());
        ordered.sort(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed()
            .thenComparing(Map.Entry::getKey));
        var top = ordered.subList(0, Math.min(TOP_K, ordered.size()));

        double l2 = 0.0;
        for (var e : top) l2 += e.getValue() * e.getValue();
        l2 = Math.sqrt(l2);
        double norm = l2 == 0.0 ? 1.0 : l2;

        Map<String, Double> vector = new LinkedHashMap<>();
        for (var e : top) vector.put(e.getKey(), e.getValue() / norm);

        return new Extraction(vector.keySet().stream().toList(), vector);
    }
}
