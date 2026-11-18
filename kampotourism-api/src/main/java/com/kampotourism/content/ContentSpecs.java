package com.kampotourism.content;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
final class ContentSpecs {
    private ContentSpecs() { }
    static Specification<ContentItem> publicListing(ContentType type, Long categoryId, Boolean featured, String q) {
        return (root, query, cb) -> {
            var p = cb.and(cb.equal(root.get("type"), type), cb.equal(root.get("status"), ContentStatus.PUBLISHED));
            if (categoryId != null) p=cb.and(p, cb.equal(root.get("categoryId"), categoryId));
            if (featured != null) p=cb.and(p, cb.equal(root.get("featured"), featured));
            if (q != null && !q.isBlank()) p=cb.and(p, matchesText(root, query.subquery(Long.class), cb, q));
            return p;
        };
    }
    static Specification<ContentItem> adminListing(ContentType type, ContentStatus status, String q) {
        return (root, query, cb) -> {
            var p=cb.equal(root.get("type"), type);
            if(status != null) p=cb.and(p, cb.equal(root.get("status"), status));
            if(q != null && !q.isBlank()) p=cb.and(p, matchesText(root, query.subquery(Long.class), cb, q));
            return p;
        };
    }
    private static jakarta.persistence.criteria.Predicate matchesText(jakarta.persistence.criteria.Root<ContentItem> root,
            Subquery<Long> names, jakarta.persistence.criteria.CriteriaBuilder cb, String q) {
        String pattern="%"+q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")+"%";
        Root<ContentTranslation> text=names.from(ContentTranslation.class);
        names.select(text.get("contentId")).where(cb.like(cb.lower(text.get("title")),pattern,'\\'));
        return cb.or(cb.like(cb.lower(root.get("slug")),pattern,'\\'),root.get("id").in(names));
    }
}
