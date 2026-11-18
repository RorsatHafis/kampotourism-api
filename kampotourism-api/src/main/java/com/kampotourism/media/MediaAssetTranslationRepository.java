package com.kampotourism.media;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetTranslationRepository extends JpaRepository<MediaAssetTranslation, Long> {

    List<MediaAssetTranslation> findByAssetId(Long assetId);

    List<MediaAssetTranslation> findByAssetIdInAndLangIn(Collection<Long> assetIds, Collection<String> langs);
}
