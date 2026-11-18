package com.kampotourism.destination;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DestinationRepository
        extends JpaRepository<Destination, Long>, JpaSpecificationExecutor<Destination> {

    Optional<Destination> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
