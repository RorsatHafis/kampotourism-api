package com.kampotourism.destination;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

/** Language-neutral destination data. Texts live in DestinationTranslation, images in the media module. */
@Entity
@Table(name = "destination")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** Legacy external cover from the demo seed; used only when no cover image is attached. */
    private String imageUrl;

    private Double latitude;
    private Double longitude;
    private BigDecimal entranceFee;
    private String entranceFeeCurrency;
    private boolean featured;
    private boolean published;
    private int sortOrder;

    @Version
    private long version;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Destination() {
    }

    void edit(String slug, Long categoryId, Double latitude, Double longitude, BigDecimal entranceFee,
              String entranceFeeCurrency, boolean featured, int sortOrder) {
        this.slug = slug;
        this.categoryId = categoryId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.entranceFee = entranceFee;
        this.entranceFeeCurrency = entranceFee == null ? null : entranceFeeCurrency;
        this.featured = featured;
        this.sortOrder = sortOrder;
    }

    void setPublished(boolean published) {
        this.published = published;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public Long getCategoryId() { return categoryId; }
    public String getImageUrl() { return imageUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public BigDecimal getEntranceFee() { return entranceFee; }
    public String getEntranceFeeCurrency() { return entranceFeeCurrency; }
    public boolean isFeatured() { return featured; }
    public boolean isPublished() { return published; }
    public int getSortOrder() { return sortOrder; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
