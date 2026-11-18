package com.kampotourism.media;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaAttachmentRepository extends JpaRepository<MediaAttachment, Long> {

    List<MediaAttachment> findByOwnerTypeAndOwnerIdInOrderBySortOrderAscIdAsc(
            OwnerType ownerType, Collection<Long> ownerIds);

    List<MediaAttachment> findByAssetId(Long assetId);

    long countByAssetId(Long assetId);

    long deleteByOwnerTypeAndOwnerId(OwnerType ownerType, Long ownerId);

    long deleteByAssetId(Long assetId);

    /** Rows of [assetId, usageCount]. */
    @Query("select a.assetId, count(a) from MediaAttachment a where a.assetId in :ids group by a.assetId")
    List<Object[]> countByAssetIds(@Param("ids") Collection<Long> ids);
}
