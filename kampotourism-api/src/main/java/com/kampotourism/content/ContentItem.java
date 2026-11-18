package com.kampotourism.content;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "content_item")
public class ContentItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ContentType type;
    @Column(nullable = false) private String slug;
    private Long categoryId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ContentStatus status = ContentStatus.DRAFT;
    private boolean featured;
    private int sortOrder;
    private String address; private Double latitude; private Double longitude;
    private BigDecimal price; private String currency; private String pricingUnit;
    private String contactPhone; private String contactEmail; private String externalUrl;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> tags;
    private String bestFor; private String authorName; private String authorUrl;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode metadata;
    private Instant publishedAt;
    @Version private long version;
    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;
    protected ContentItem() { }
    ContentItem(ContentType type, String slug, Long categoryId, boolean featured, int sortOrder,
                Double latitude, Double longitude, BigDecimal price, String currency, String pricingUnit,
                String address, String contactPhone, String contactEmail, String externalUrl, List<String> tags,
                String bestFor, String authorName, String authorUrl, JsonNode metadata) {
        this.type = type; edit(slug, categoryId, featured, sortOrder, latitude, longitude, price, currency,
                pricingUnit, address, contactPhone, contactEmail, externalUrl, tags, bestFor, authorName, authorUrl, metadata);
    }
    void edit(String slug, Long categoryId, boolean featured, int sortOrder, Double latitude, Double longitude,
              BigDecimal price, String currency, String pricingUnit, String address, String contactPhone,
              String contactEmail, String externalUrl, List<String> tags, String bestFor, String authorName,
              String authorUrl, JsonNode metadata) {
        this.slug=slug; this.categoryId=categoryId; this.featured=featured; this.sortOrder=sortOrder;
        this.latitude=latitude; this.longitude=longitude; this.price=price; this.currency=price == null ? null : currency;
        this.pricingUnit=pricingUnit; this.address=address; this.contactPhone=contactPhone; this.contactEmail=contactEmail;
        this.externalUrl=externalUrl; this.tags=tags == null ? List.of() : List.copyOf(tags); this.bestFor=bestFor;
        this.authorName=authorName; this.authorUrl=authorUrl; this.metadata=metadata;
    }
    void setStatus(ContentStatus status) { this.status=status; this.publishedAt=status == ContentStatus.PUBLISHED ? Instant.now() : null; }
    public Long getId(){return id;} public ContentType getType(){return type;} public String getSlug(){return slug;}
    public Long getCategoryId(){return categoryId;} public ContentStatus getStatus(){return status;} public boolean isFeatured(){return featured;}
    public int getSortOrder(){return sortOrder;} public String getAddress(){return address;} public Double getLatitude(){return latitude;}
    public Double getLongitude(){return longitude;} public BigDecimal getPrice(){return price;} public String getCurrency(){return currency;}
    public String getPricingUnit(){return pricingUnit;} public String getContactPhone(){return contactPhone;}
    public String getContactEmail(){return contactEmail;} public String getExternalUrl(){return externalUrl;} public JsonNode getMetadata(){return metadata;}
    public List<String> getTags(){return tags;} public String getBestFor(){return bestFor;} public String getAuthorName(){return authorName;} public String getAuthorUrl(){return authorUrl;}
    public Instant getPublishedAt(){return publishedAt;} public long getVersion(){return version;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
