package com.kampotourism.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_asset_translation")
public class MediaAssetTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private String lang;

    private String altText;
    private String caption;

    protected MediaAssetTranslation() {
    }

    MediaAssetTranslation(Long assetId, String lang) {
        this.assetId = assetId;
        this.lang = lang;
    }

    void edit(String altText, String caption) {
        this.altText = clean(altText);
        this.caption = clean(caption);
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getAssetId() { return assetId; }
    public String getLang() { return lang; }
    public String getAltText() { return altText; }
    public String getCaption() { return caption; }
}
