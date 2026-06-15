package com.sisibibi.api.global.moderation;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeywordProfanityDetector implements ProfanityDetector {

    private static final String[] CHOSEONG = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
            "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };
    private static final String[] JUNGSEONG = {
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
            "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    };
    private static final String[] JONGSEONG = {
            "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ",
            "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ",
            "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    private static final String PROFANITY_WORDS_PATH = "moderation/profanity-words.txt";
    private static final String SEXUAL_HARASSMENT_PHRASES_PATH =
            "moderation/sexual-harassment-phrases.txt";
    private static final String ALLOWED_PHRASES_PATH = "moderation/profanity-allowed-phrases.txt";
    private static final String EVASION_ALIASES_PATH = "moderation/evasion-aliases.txt";

    private final Set<String> blockedTerms;
    private final Set<String> allowedPhrases;
    private final List<NormalizationAlias> evasionAliases;

    public KeywordProfanityDetector() {
        this.blockedTerms = new LinkedHashSet<>(loadNormalizedTerms(PROFANITY_WORDS_PATH));
        this.blockedTerms.addAll(loadNormalizedTerms(SEXUAL_HARASSMENT_PHRASES_PATH));
        this.allowedPhrases = loadNormalizedTerms(ALLOWED_PHRASES_PATH);
        this.evasionAliases = loadNormalizationAliases(EVASION_ALIASES_PATH);
    }

    @Override
    public boolean containsProfanity(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        return createNormalizedCandidates(content).stream()
                .map(this::removeAllowedPhrases)
                .anyMatch(this::containsBlockedTerm);
    }

    private Set<String> createNormalizedCandidates(String content) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalize(content));
        expandCandidates(candidates, this::foldHangulJamo);
        expandCandidates(candidates, value -> value.replaceAll("\\p{N}", ""));
        expandCandidates(candidates, this::collapseRepeatedCharacters);
        expandCandidates(candidates, this::applyEvasionAliases);
        return candidates;
    }

    private void expandCandidates(Set<String> candidates, UnaryOperator<String> normalizer) {
        candidates.stream()
                .map(normalizer)
                .filter(candidate -> !candidate.isBlank())
                .toList()
                .forEach(candidates::add);
    }

    private String collapseRepeatedCharacters(String content) {
        StringBuilder collapsed = new StringBuilder();
        int previousCodePoint = -1;

        for (int index = 0; index < content.length();) {
            int codePoint = content.codePointAt(index);
            if (codePoint != previousCodePoint) {
                collapsed.appendCodePoint(codePoint);
            }
            previousCodePoint = codePoint;
            index += Character.charCount(codePoint);
        }

        return collapsed.toString();
    }

    private String applyEvasionAliases(String content) {
        String normalizedContent = content;
        for (NormalizationAlias alias : evasionAliases) {
            normalizedContent = normalizedContent.replace(alias.source(), alias.target());
        }
        return normalizedContent;
    }

    private boolean containsBlockedTerm(String content) {
        return blockedTerms.stream().anyMatch(content::contains);
    }

    private String removeAllowedPhrases(String content) {
        String filteredContent = content;
        for (String allowedPhrase : allowedPhrases) {
            filteredContent = filteredContent.replace(allowedPhrase, "");
        }
        return filteredContent;
    }

    private Set<String> loadNormalizedTerms(String path) {
        return readResourceLines(path).stream()
                .map(this::normalize)
                .filter(term -> !term.isBlank())
                .flatMap(term -> Stream.of(term, foldHangulJamo(term)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<NormalizationAlias> loadNormalizationAliases(String path) {
        return readResourceLines(path).stream()
                .map(line -> line.split("=>", 2))
                .filter(parts -> parts.length == 2)
                .map(parts -> new NormalizationAlias(
                        normalize(parts[0].trim()),
                        normalize(parts[1].trim())
                ))
                .filter(alias -> !alias.source().isBlank() && !alias.target().isBlank())
                .sorted((left, right) -> Integer.compare(
                        right.source().length(),
                        left.source().length()
                ))
                .toList();
    }

    private List<String> readResourceLines(String path) {
        InputStream inputStream = KeywordProfanityDetector.class.getClassLoader()
                .getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalStateException("욕설 탐지 리소스 파일을 찾을 수 없습니다: " + path);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)
        )) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("#"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("욕설 탐지 리소스 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    private String normalize(String value) {
        String compatibilityNormalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        return Normalizer.normalize(compatibilityNormalized, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String foldHangulJamo(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD);
        StringBuilder folded = new StringBuilder();

        for (int index = 0; index < decomposed.length();) {
            int codePoint = decomposed.codePointAt(index);
            if (codePoint >= 0x1100 && codePoint <= 0x1112) {
                folded.append(CHOSEONG[codePoint - 0x1100]);
            } else if (codePoint >= 0x1161 && codePoint <= 0x1175) {
                folded.append(JUNGSEONG[codePoint - 0x1161]);
            } else if (codePoint >= 0x11A8 && codePoint <= 0x11C2) {
                folded.append(JONGSEONG[codePoint - 0x11A8]);
            } else {
                folded.appendCodePoint(codePoint);
            }
            index += Character.charCount(codePoint);
        }

        return folded.toString();
    }

    private record NormalizationAlias(String source, String target) {
    }
}
