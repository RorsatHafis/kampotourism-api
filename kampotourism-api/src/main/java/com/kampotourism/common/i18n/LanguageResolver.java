package com.kampotourism.common.i18n;

import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.config.I18nProperties;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Decides which language a request wants: the explicit {@code ?lang=} parameter
 * wins, then the Accept-Language header (respecting q-values), then the default.
 * Only languages listed in {@code app.i18n.supported} are ever returned.
 */
@Component
public class LanguageResolver {

    private final I18nProperties properties;
    private final Set<String> supported;

    public LanguageResolver(I18nProperties properties) {
        this.properties = properties;
        this.supported = properties.supported().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String defaultLang() {
        return properties.defaultLang();
    }

    /**
     * For admin writes: the tag must be exactly one of the supported languages
     * (no primary-tag fallback), otherwise editors could create rows nobody can read.
     */
    public String requireSupported(String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (!supported.contains(normalized)) {
            throw new BadRequestException("Unsupported language '%s'. Supported: %s"
                    .formatted(tag, properties.supported()));
        }
        return normalized;
    }

    public String resolve(String langParam, String acceptLanguage) {
        if (langParam != null && !langParam.isBlank()) {
            String match = match(langParam);
            if (match != null) {
                return match;
            }
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage)) {
                    String match = match(range.getRange());
                    if (match != null) {
                        return match;
                    }
                }
            } catch (IllegalArgumentException malformedHeader) {
                // A malformed header must never fail the request; fall through to the default.
            }
        }
        return properties.defaultLang();
    }

    /** "zh-CN" matches "zh-cn" if supported, otherwise its primary tag "zh". */
    private String match(String tag) {
        String normalized = tag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (supported.contains(normalized)) {
            return normalized;
        }
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            String primary = normalized.substring(0, dash);
            if (supported.contains(primary)) {
                return primary;
            }
        }
        return null;
    }
}
