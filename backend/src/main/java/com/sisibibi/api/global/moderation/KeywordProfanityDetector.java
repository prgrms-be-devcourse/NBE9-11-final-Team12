package com.sisibibi.api.global.moderation;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KeywordProfanityDetector implements ProfanityDetector {

    private static final String PROFANITY_WORDS_PATH = "moderation/profanity-words.txt";
    private static final String SEXUAL_HARASSMENT_PHRASES_PATH =
            "moderation/sexual-harassment-phrases.txt";
    private static final String ALLOWED_PHRASES_PATH = "moderation/profanity-allowed-phrases.txt";

    private final Set<String> blockedTerms;
    private final Set<String> allowedPhrases;

    public KeywordProfanityDetector() {
        this.blockedTerms = new LinkedHashSet<>(loadNormalizedTerms(PROFANITY_WORDS_PATH));
        this.blockedTerms.addAll(loadNormalizedTerms(SEXUAL_HARASSMENT_PHRASES_PATH));
        this.allowedPhrases = loadNormalizedTerms(ALLOWED_PHRASES_PATH);
    }

    @Override
    public boolean containsProfanity(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String normalizedContent = removeAllowedPhrases(normalize(content));
        return blockedTerms.stream().anyMatch(normalizedContent::contains);
    }

    private String removeAllowedPhrases(String content) {
        String filteredContent = content;
        for (String allowedPhrase : allowedPhrases) {
            filteredContent = filteredContent.replace(allowedPhrase, "");
        }
        return filteredContent;
    }

    private Set<String> loadNormalizedTerms(String path) {
        InputStream inputStream = KeywordProfanityDetector.class.getClassLoader()
                .getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalStateException("욕설 탐지 사전 파일을 찾을 수 없습니다: " + path);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)
        )) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("#"))
                    .map(this::normalize)
                    .filter(term -> !term.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException("욕설 탐지 사전 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
