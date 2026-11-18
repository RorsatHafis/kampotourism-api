package com.kampotourism.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Links an asset to a piece of content as its cover or as a gallery image. */
@Entity
@Table(name = "media_attachment")
public class MediaAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnerType ownerType;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long assetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentRole role;

    private int sortOrder;

    protected MediaAttachment() {
    }

    MediaAttachment(OwnerType ownerType, Long ownerId, Long assetId, AttachmentRole role, int sortOrder) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.assetId = assetId;
        this.role = role;
        this.sortOrder = sortOrder;
    }

    public OwnerType getOwnerType() { return ownerType; }
    public Long getOwnerId() { return ownerId; }
    public Long getAssetId() { return assetId; }
    public AttachmentRole getRole() { return role; }
    public int getSortOrder() { return sortOrder; }
}
