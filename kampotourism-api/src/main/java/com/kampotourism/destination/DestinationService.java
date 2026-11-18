package com.kampotourism.destination;

import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.category.CategoryKind;
import com.kampotourism.category.CategoryService;
import com.kampotourism.common.PageResponse;
import com.kampotourism.common.error.NotFoundException;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.destination.DestinationDtos.DestinationCard;
import com.kampotourism.destination.DestinationDtos.DestinationDetail;
import com.kampotourism.media.MediaAttachmentService;
import com.kampotourism.media.MediaDtos.OwnerImages;
import com.kampotourism.media.OwnerType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public, read-only side of destinations. */
@Service
@Transactional(readOnly = true)
public class DestinationService {

    static final int MAX_PAGE_SIZE = 50;
    private static final int RELATED_COUNT = 2;
    private static final Sort DEFAULT_SORT = Sort.by("sortOrder", "id");

    private final DestinationRepository destinations;
    private final DestinationTranslationRepository translations;
    private final CategoryService categories;
    private final MediaAttachmentService images;
    private final LanguageResolver languages;

    public DestinationService(DestinationRepository destinations, DestinationTranslationRepository translations,
                              CategoryService categories, MediaAttachmentService images,
                              LanguageResolver languages) {
        this.destinations = destinations;
        this.translations = translations;
        this.categories = categories;
        this.images = images;
        this.languages = languages;
    }

    public PageResponse<DestinationCard> list(String categorySlug, Boolean featured, int page, int size, String lang) {
        return list(categorySlug, featured, null, page, size, lang);
    }

    public PageResponse<DestinationCard> list(String categorySlug, Boolean featured, String q, int page, int size, String lang) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long categoryId = (categorySlug == null || categorySlug.isBlank()) ? null
                : categories.idForSlug(CategoryKind.DESTINATION, categorySlug);

        Page<Destination> result = destinations.findAll(
                DestinationSpecs.listing(categoryId, featured, q),
                PageRequest.of(safePage - 1, safeSize, DEFAULT_SORT));

        Lookups lookups = lookups(result.getContent(), lang);
        List<DestinationCard> items = result.getContent().stream()
                .filter(d -> lookups.text().containsKey(d.getId()))
                .map(lookups::card)
                .toList();
        return new PageResponse<>(items, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    public DestinationDetail get(String slug, String lang) {
        Destination destination = destinations.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> notFound(slug));
        List<Destination> related = destinations.findAll(
                DestinationSpecs.publishedExcept(destination.getId()),
                PageRequest.of(0, RELATED_COUNT, DEFAULT_SORT)).getContent();

        List<Destination> all = new ArrayList<>();
        all.add(destination);
        all.addAll(related);
        Lookups lookups = lookups(all, lang);

        DestinationTranslation own = lookups.text().get(destination.getId());
        if (own == null) {
            throw notFound(slug); // no content in the requested or default language
        }
        List<DestinationCard> relatedCards = related.stream()
                .filter(d -> lookups.text().containsKey(d.getId()))
                .map(lookups::card)
                .toList();
        return DestinationMapper.toDetail(destination, own, lookups.category(destination),
                lookups.images().get(destination.getId()), relatedCards);
    }

    /** Everything needed to render a batch of destinations, fetched in a fixed number of queries. */
    private record Lookups(Map<Long, DestinationTranslation> text, Map<Long, CategoryDto> categories,
                           Map<Long, OwnerImages> images) {

        CategoryDto category(Destination d) {
            return categories.get(d.getCategoryId());
        }

        DestinationCard card(Destination d) {
            return DestinationMapper.toCard(d, text.get(d.getId()), category(d), images.get(d.getId()));
        }
    }

    private Lookups lookups(Collection<Destination> items, String lang) {
        List<Long> ids = items.stream().map(Destination::getId).toList();
        Set<Long> categoryIds = items.stream().map(Destination::getCategoryId).collect(Collectors.toSet());
        return new Lookups(translationsFor(ids, lang),
                categories.byIds(categoryIds, lang),
                images.publicImages(OwnerType.DESTINATION, ids, lang));
    }

    /** One query for all translations, then per destination: requested language if present, else the default. */
    private Map<Long, DestinationTranslation> translationsFor(Collection<Long> ids, String lang) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Set<String> wanted = Set.copyOf(List.of(lang, languages.defaultLang()));
        Map<Long, DestinationTranslation> picked = new HashMap<>();
        for (DestinationTranslation t : translations.findByDestinationIdInAndLangIn(ids, wanted)) {
            if (t.getLang().equals(lang) || !picked.containsKey(t.getDestinationId())) {
                picked.put(t.getDestinationId(), t);
            }
        }
        return picked;
    }

    private static NotFoundException notFound(String slug) {
        return new NotFoundException("Destination '%s' was not found".formatted(slug));
    }
}
