package com.sisibibi.api.global.moderation;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordProfanityDetectorTest {

    private final KeywordProfanityDetector profanityDetector = new KeywordProfanityDetector();

    @Test
    void profanityDictionary_containsCuratedOperationalTerms() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                KeywordProfanityDetectorTest.class.getClassLoader()
                        .getResourceAsStream("moderation/profanity-words.txt"),
                StandardCharsets.UTF_8
        ))) {
            long termCount = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("#"))
                    .count();

            assertThat(termCount).isGreaterThanOrEqualTo(100);
        }
    }

    @Test
    void containsProfanity_returnsTrue_whenContentContainsRegisteredWord() {
        assertThat(profanityDetector.containsProfanity("진짜 씨발 말이 안 됩니다."))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_whenProfanityIsObfuscatedWithSeparators() {
        assertThat(profanityDetector.containsProfanity("씨-발이라고 쓰면 괜찮나"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("개 새 끼"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("ㅆ ㅂ"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_forCuratedDatasetVariants() {
        assertThat(profanityDetector.containsProfanity("그건 씨벌 말도 안 돼"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("완전 좃깟네"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("상대에게 엿먹어라라고 했다"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("fuck you"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_forObfuscatedProfanityFromProvidedDataset() {
        assertThat(profanityDetector.containsProfanity("10새끼"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("^^ㅣ발"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("10JIL"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_whenDigitsAreInsertedInsideProfanity() {
        assertThat(profanityDetector.containsProfanity("시123발"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("ㅅ1ㅂ"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_whenKoreanAndRomanizedEvasionAreMixed() {
        assertThat(profanityDetector.containsProfanity("niㅇh미"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("sibal"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("tlqkf"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("SsIbAl"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("qudtls"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_whenUnicodeOrRepeatedCharactersAreUsed() {
        assertThat(profanityDetector.containsProfanity("시\u200B발"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("ㅅㅣㅂㅏㄹ"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("씨씨발"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsTrue_forSexualHarassmentPhrases() {
        assertThat(profanityDetector.containsProfanity("너 가슴 빨아"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("니 보지 구멍"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("몸 안에 사정"))
                .isTrue();
        assertThat(profanityDetector.containsProfanity("걸레 같은 년"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsFalse_whenContentDoesNotContainProfanityOrMatchesAllowedPhrase() {
        assertThat(profanityDetector.containsProfanity("상대 의견의 근거가 부족합니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("프로젝트의 시발점입니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("시123발점이라는 표현도 정상 문맥입니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("2026년에는 123명이 참여했습니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("minimal design is important"))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("ㅋㅋㅋㅋ 의견이 재미있네요"))
                .isFalse();
    }

    @Test
    void containsProfanity_returnsTrue_whenAllowedPhraseAndProfanityAppearTogether() {
        assertThat(profanityDetector.containsProfanity("프로젝트의 시발점인데 진짜 시발"))
                .isTrue();
    }

    @Test
    void containsProfanity_returnsFalse_forTermsExcludedFromUpstreamDataset() {
        assertThat(profanityDetector.containsProfanity("공지사항은 운영자가 작성합니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("성교육에서 성교와 성폭행의 차이를 설명합니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("트랜스젠더와 동성애자의 인권을 토론합니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("성폭행 피해자 보호 정책을 논의합니다."))
                .isFalse();
        assertThat(profanityDetector.containsProfanity("성교육에서 신체 부위와 성교를 설명합니다."))
                .isFalse();
    }

    @Test
    void containsProfanity_returnsFalse_whenContentIsNullOrBlank() {
        assertThat(profanityDetector.containsProfanity(null)).isFalse();
        assertThat(profanityDetector.containsProfanity("   ")).isFalse();
    }
}
