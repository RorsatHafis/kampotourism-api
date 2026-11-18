package com.kampotourism.category;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, Long> {

    List<CategoryTranslation> findByCategoryId(Long categoryId);

    Optional<CategoryTranslation> findByCategoryIdAndLang(Long categoryId, String lang);

    List<CategoryTranslation> findByCategoryIdInAndLangIn(Collection<Long> categoryIds, Collection<String> langs);

    long deleteByCategoryIdAndLang(Long categoryId, String lang);
}
