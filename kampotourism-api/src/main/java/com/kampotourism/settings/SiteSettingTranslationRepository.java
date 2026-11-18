package com.kampotourism.settings;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SiteSettingTranslationRepository extends JpaRepository<SiteSettingTranslation, Long> {
    Optional<SiteSettingTranslation> findBySettingKeyAndLang(String settingKey, String lang);
    List<SiteSettingTranslation> findBySettingKeyAndLangIn(String settingKey, Collection<String> languages);
}
