package com.kampotourism.content;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface ContentItemRepository extends JpaRepository<ContentItem, Long>, JpaSpecificationExecutor<ContentItem> {
    Optional<ContentItem> findByTypeAndSlugAndStatus(ContentType type, String slug, ContentStatus status);
    boolean existsByTypeAndSlug(ContentType type, String slug);
    boolean existsByTypeAndSlugAndIdNot(ContentType type, String slug, Long id);
}
