package com.kampotourism.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Page<MediaAsset> findByOriginalFilenameContainingIgnoreCase(String filename, Pageable pageable);
}
