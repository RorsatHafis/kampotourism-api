package com.kampotourism.destination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationTranslationRepository extends JpaRepository<DestinationTranslation, Long> {

    List<DestinationTranslation> findByDestinationIdInAndLangIn(
            Collection<Long> destinationIds, Collection<String> langs);

    List<DestinationTranslation> findByDestinationId(Long destinationId);

    List<DestinationTranslation> findByDestinationIdIn(Collection<Long> destinationIds);

    Optional<DestinationTranslation> findByDestinationIdAndLang(Long destinationId, String lang);

    long deleteByDestinationIdAndLang(Long destinationId, String lang);
}
