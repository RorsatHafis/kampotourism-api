package com.kampotourism.content;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentTranslationRepository extends JpaRepository<ContentTranslation, Long> {
    List<ContentTranslation> findByContentIdInAndLangIn(Collection<Long> ids, Collection<String> langs);
    List<ContentTranslation> findByContentIdIn(Collection<Long> ids);
    List<ContentTranslation> findByContentId(Long id);
    Optional<ContentTranslation> findByContentIdAndLang(Long id, String lang);
    void deleteByContentIdAndLang(Long id, String lang);
}
