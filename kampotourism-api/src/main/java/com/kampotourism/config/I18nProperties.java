package com.kampotourism.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.i18n")
public record I18nProperties(String defaultLang, List<String> supported) {

    public I18nProperties {
        if (defaultLang == null || defaultLang.isBlank()) {
            defaultLang = "en";
        }
        if (supported == null || supported.isEmpty()) {
            supported = List.of(defaultLang);
        }
    }
}
