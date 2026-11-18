package com.kampotourism.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByKindAscSortOrderAscIdAsc();

    List<Category> findByKindOrderBySortOrderAscIdAsc(CategoryKind kind);

    Optional<Category> findByKindAndSlug(CategoryKind kind, String slug);

    Optional<Category> findByIdAndKind(Long id, CategoryKind kind);

    boolean existsByKindAndSlug(CategoryKind kind, String slug);

    boolean existsByKindAndSlugAndIdNot(CategoryKind kind, String slug, Long id);
}
