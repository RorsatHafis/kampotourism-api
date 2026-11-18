package com.kampotourism.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

/** One uploaded file. Alt text and captions live in MediaAssetTranslation. */
@Entity
@Table(name = "media_asset")
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String storageKey;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long sizeBytes;

    private Integer width;
    private Integer height;
    private String originalFilename;

    @Version
    private long version;

    @UpdateTimestamp
    private Instant updatedAt;

    protected MediaAsset() {
    }

    MediaAsset(MediaStorage.StoredFile file) {
        replaceFile(file);
    }

    void replaceFile(MediaStorage.StoredFile file) {
        this.storageKey = file.key();
        this.mimeType = file.mimeType();
        this.sizeBytes = file.sizeBytes();
        this.width = file.width();
        this.height = file.height();
        this.originalFilename = file.originalFilename();
    }

    public Long getId() { return id; }
    public String getStorageKey() { return storageKey; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public String getOriginalFilename() { return originalFilename; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
