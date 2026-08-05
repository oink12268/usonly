package com.evho.usonly.global.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Test
    void roundTripsPlainText() {
        String original = "오늘 저녁 뭐 먹지? 🍜 IMAGE:https://example.com/a.jpg 테스트";
        String encrypted = converter.convertToDatabaseColumn(original);

        assertThat(encrypted).startsWith("enc:");
        assertThat(encrypted).isNotEqualTo(original);

        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void legacyPlainTextPassesThroughUnchanged() {
        String legacy = "암호화 이전에 저장된 평문";
        assertThat(converter.convertToEntityAttribute(legacy)).isEqualTo(legacy);
    }

    @Test
    void nullPassesThrough() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void sameInputProducesDifferentCiphertextEachTime() {
        String original = "매번 다른 IV";
        String a = converter.convertToDatabaseColumn(original);
        String b = converter.convertToDatabaseColumn(original);
        assertThat(a).isNotEqualTo(b); // 랜덤 IV라 매번 달라야 함
        assertThat(converter.convertToEntityAttribute(a)).isEqualTo(original);
        assertThat(converter.convertToEntityAttribute(b)).isEqualTo(original);
    }
}
