package com.kampotourism.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.config.I18nProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class LanguageResolverTest {

    private final LanguageResolver resolver =
            new LanguageResolver(new I18nProperties("en", List.of("en", "km", "zh")));

    @Test
    void explicitParameterWinsOverHeader() {
        assertThat(resolver.resolve("km", "zh,en;q=0.8")).isEqualTo("km");
    }

    @Test
    void headerIsUsedInQualityOrder() {
        assertThat(resolver.resolve(null, "fr;q=0.9,zh;q=0.8,en;q=0.5")).isEqualTo("zh");
    }

    @Test
    void regionTagFallsBackToPrimaryLanguage() {
        assertThat(resolver.resolve(null, "zh-CN,zh;q=0.9")).isEqualTo("zh");
        assertThat(resolver.resolve("km_KH", null)).isEqualTo("km");
    }

    @Test
    void unsupportedOrMissingLanguageUsesDefault() {
        assertThat(resolver.resolve("de", "de-DE,de;q=0.9")).isEqualTo("en");
        assertThat(resolver.resolve(null, null)).isEqualTo("en");
        assertThat(resolver.resolve("", "")).isEqualTo("en");
    }

    @Test
    void malformedHeaderNeverFails() {
        assertThat(resolver.resolve(null, "???;;;")).isEqualTo("en");
    }
}
