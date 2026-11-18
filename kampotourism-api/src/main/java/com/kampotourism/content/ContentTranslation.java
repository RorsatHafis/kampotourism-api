package com.kampotourism.content;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "content_item_translation")
public class ContentTranslation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long contentId;
    @Column(nullable = false) private String lang;
    @Column(nullable = false) private String title; private String excerpt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode body;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode highlights;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode tips;
    private String seoTitle; private String seoDescription; private boolean confirmedInformation = true;
    protected ContentTranslation() { }
    ContentTranslation(Long contentId, String lang) { this.contentId=contentId; this.lang=lang; }
    void edit(String title, String excerpt, JsonNode body, JsonNode highlights, JsonNode tips,
              String seoTitle, String seoDescription, boolean confirmedInformation) {
        this.title=title; this.excerpt=excerpt; this.body=body; this.highlights=highlights; this.tips=tips;
        this.seoTitle=seoTitle; this.seoDescription=seoDescription; this.confirmedInformation=confirmedInformation;
    }
    public Long getContentId(){return contentId;} public String getLang(){return lang;} public String getTitle(){return title;}
    public String getExcerpt(){return excerpt;} public JsonNode getBody(){return body;} public JsonNode getHighlights(){return highlights;}
    public JsonNode getTips(){return tips;} public String getSeoTitle(){return seoTitle;} public String getSeoDescription(){return seoDescription;}
    public boolean isConfirmedInformation(){return confirmedInformation;}
}
