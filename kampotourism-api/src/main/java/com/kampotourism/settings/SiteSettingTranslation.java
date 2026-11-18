package com.kampotourism.settings;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "site_setting_translation")
public class SiteSettingTranslation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String settingKey;
    @Column(nullable = false) private String lang;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode value;
    protected SiteSettingTranslation() { }
    SiteSettingTranslation(String settingKey, String lang, JsonNode value) { this.settingKey=settingKey; this.lang=lang; this.value=value; }
    void setValue(JsonNode value) { this.value=value; }
    public String getLang() { return lang; }
    public JsonNode getValue() { return value; }
}
