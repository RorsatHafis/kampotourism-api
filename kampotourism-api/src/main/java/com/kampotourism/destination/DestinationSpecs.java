package com.kampotourism.destination;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class DestinationSpecs {

    private DestinationSpecs() {
    }

    /** Public listing: published only, optionally narrowed by category and featured flag. */
    static Specification<Destination> listing(Long categoryId, Boolean featured, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("published")));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                Subquery<Long> names = query.subquery(Long.class);
                Root<DestinationTranslation> translation = names.from(DestinationTranslation.class);
                names.select(translation.<Long>get("destinationId"))
                        .where(cb.like(cb.lower(translation.<String>get("name")), pattern, '\\'));
                predicates.add(cb.or(cb.like(cb.lower(root.<String>get("slug")), pattern, '\\'), root.get("id").in(names)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static Specification<Destination> publishedExcept(Long id) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("published")),
                cb.notEqual(root.get("id"), id));
    }

    /** Admin listing: drafts included; q matches the slug or any translated name. */
    static Specification<Destination> admin(Boolean published, Long categoryId, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (published != null) {
                predicates.add(cb.equal(root.get("published"), published));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                Subquery<Long> names = query.subquery(Long.class);
                Root<DestinationTranslation> translation = names.from(DestinationTranslation.class);
                names.select(translation.<Long>get("destinationId"))
                        .where(cb.like(cb.lower(translation.<String>get("name")), pattern, '\\'));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("slug")), pattern, '\\'),
                        root.get("id").in(names)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
