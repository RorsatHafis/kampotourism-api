package com.kampotourism.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.common.error.ConflictException;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.content.ContentService;
import com.kampotourism.content.ContentType;
import com.kampotourism.destination.DestinationDtos;
import com.kampotourism.destination.DestinationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HomepageService {
    private static final String HOMEPAGE = "homepage";
    private final SiteSettingRepository settings;
    private final SiteSettingTranslationRepository translations;
    private final DestinationService destinations;
    private final ContentService content;
    private final LanguageResolver languages;

    public HomepageService(SiteSettingRepository settings, SiteSettingTranslationRepository translations,
                           DestinationService destinations, ContentService content, LanguageResolver languages) {
        this.settings=settings; this.translations=translations; this.destinations=destinations; this.content=content; this.languages=languages;
    }
    @Transactional(readOnly = true)
    public Home view(String lang) {
        return new Home(configuration(lang), destinations.list(null,true,1,12,lang).items(),
                content.list(ContentType.EXPERIENCE,null,true,null,1,12,lang).items(),
                content.list(ContentType.ACCOMMODATION,null,true,null,1,12,lang).items(),
                content.list(ContentType.ARTICLE,null,true,null,1,12,lang).items(),lang);
    }
    @Transactional(readOnly = true)
    public AdminHome admin(String lang) { SiteSetting setting=homepage(); return new AdminHome(configuration(lang),setting.getVersion(),setting.getUpdatedAt(),lang); }
    public AdminHome update(long version, String lang, JsonNode value) {
        if (!value.isObject()) throw new BadRequestException("Homepage configuration must be a JSON object.");
        SiteSetting setting=homepage();
        if(setting.getVersion()!=version) throw new ConflictException("Homepage settings were changed by someone else. Reload and try again.");
        SiteSettingTranslation translation=translations.findBySettingKeyAndLang(HOMEPAGE,lang)
                .orElseGet(()->new SiteSettingTranslation(HOMEPAGE,lang,value));
        translation.setValue(value); translations.save(translation);
        if (lang.equals(languages.defaultLang())) setting.setValue(value);
        settings.saveAndFlush(setting);
        return admin(lang);
    }
    private SiteSetting homepage() { return settings.findById(HOMEPAGE).orElseThrow(() -> new IllegalStateException("Homepage setting is missing")); }
    private JsonNode configuration(String lang) {
        SiteSetting setting=homepage();
        Map<String, SiteSettingTranslation> byLanguage=translations.findBySettingKeyAndLangIn(HOMEPAGE,Set.copyOf(List.of(lang,languages.defaultLang()))).stream()
                .collect(java.util.stream.Collectors.toMap(SiteSettingTranslation::getLang, x -> x));
        SiteSettingTranslation picked=byLanguage.getOrDefault(lang, byLanguage.get(languages.defaultLang()));
        return picked == null ? setting.getValue() : picked.getValue();
    }
    public record Home(JsonNode configuration, List<DestinationDtos.DestinationCard> featuredDestinations,
                       List<com.kampotourism.content.ContentDtos.ContentCard> featuredExperiences,
                       List<com.kampotourism.content.ContentDtos.ContentCard> featuredAccommodations,
                       List<com.kampotourism.content.ContentDtos.ContentCard> featuredArticles, String language) { }
    public record AdminHome(JsonNode configuration, long version, Instant updatedAt, String language) { }
}
