
package com.kampotourism.content;

import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.category.CategoryService;
import com.kampotourism.common.PageResponse;
import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.common.error.ConflictException;
import com.kampotourism.common.error.NotFoundException;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.content.ContentDtos.*;
import com.kampotourism.itinerary.ItineraryActivityRepository;
import com.kampotourism.media.MediaAttachmentService;
import com.kampotourism.media.MediaDtos.OwnerImages;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ContentItemRepository items;
    private final ContentTranslationRepository translations;
    private final CategoryService categories;
    private final MediaAttachmentService images;
    private final LanguageResolver languages;
    private final ItineraryActivityRepository itineraryActivities;

    public ContentService(
            ContentItemRepository items,
            ContentTranslationRepository translations,
            CategoryService categories,
            MediaAttachmentService images,
            LanguageResolver languages,
            ItineraryActivityRepository itineraryActivities
    ) {
        this.items = items;
        this.translations = translations;
        this.categories = categories;
        this.images = images;
        this.languages = languages;
        this.itineraryActivities = itineraryActivities;
    }

    @Transactional(readOnly = true)
    public PageResponse<ContentCard> list(
            ContentType type,
            String category,
            Boolean featured,
            String q,
            int page,
            int size,
            String lang
    ) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        Long categoryId = category == null || category.isBlank()
                ? null
                : categories.idForSlug(type.categoryKind(), category);

        Page<ContentItem> result = items.findAll(
                ContentSpecs.publicListing(type, categoryId, featured, q),
                PageRequest.of(
                        safePage - 1,
                        safeSize,
                        Sort.by("sortOrder", "id")
                )
        );

        Lookups lookups = lookups(result.getContent(), lang);

        List<ContentCard> cards = result.getContent()
                .stream()
                .filter(item -> lookups.text().containsKey(item.getId()))
                .map(lookups::card)
                .toList();

        return new PageResponse<>(
                cards,
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ContentDetail get(ContentType type, String slug, String lang) {
        ContentItem item = items.findByTypeAndSlugAndStatus(
                        type,
                        slug,
                        ContentStatus.PUBLISHED
                )
                .orElseThrow(() ->
                        new NotFoundException("Content '" + slug + "' was not found")
                );

        Lookups lookups = lookups(List.of(item), lang);
        ContentTranslation translation = lookups.text().get(item.getId());

        if (translation == null) {
            throw new NotFoundException("Content '" + slug + "' was not found");
        }

        OwnerImages media = lookups.images().getOrDefault(
                item.getId(),
                new OwnerImages(null, List.of())
        );

        return new ContentDetail(
                item.getSlug(),
                translation.getTitle(),
                translation.getExcerpt(),
                translation.getBody(),
                translation.getHighlights(),
                translation.getTips(),
                item.getCategoryId() == null
                        ? null
                        : lookups.categories().get(item.getCategoryId()),
                new Coordinates(item.getLatitude(), item.getLongitude()),
                item.getAddress(),
                new Price(item.getPrice(), item.getCurrency(), item.getPricingUnit()),
                item.getContactPhone(),
                item.getContactEmail(),
                item.getExternalUrl(),
                item.getMetadata(),
                item.getTags(),
                item.getBestFor(),
                item.getAuthorName(),
                item.getAuthorUrl(),
                translation.isConfirmedInformation(),
                media.cover(),
                media.gallery(),
                new Seo(
                        translation.getSeoTitle(),
                        translation.getSeoDescription(),
                        item.getSlug(),
                        media.cover()
                ),
                item.getPublishedAt(),
                translation.getLang()
        );
    }

    public AdminDetail create(ContentType type, CreateRequest request) {
        validate(
                type,
                request.categoryId(),
                request.latitude(),
                request.longitude(),
                request.price(),
                request.currency()
        );

        if (items.existsByTypeAndSlug(type, request.slug())) {
            throw new ConflictException(
                    "Slug '" + request.slug() + "' is already used by another "
                            + type.path() + " item"
            );
        }

        ContentItem item = items.saveAndFlush(
                new ContentItem(
                        type,
                        request.slug(),
                        request.categoryId(),
                        request.featured(),
                        request.sortOrder(),
                        request.latitude(),
                        request.longitude(),
                        request.price(),
                        request.currency(),
                        request.pricingUnit(),
                        request.address(),
                        request.contactPhone(),
                        request.contactEmail(),
                        request.externalUrl(),
                        request.tags(),
                        request.bestFor(),
                        request.authorName(),
                        request.authorUrl(),
                        request.metadata()
                )
        );

        saveTranslation(
                item.getId(),
                languages.defaultLang(),
                request.translation()
        );

        return adminDetail(item);
    }

    public AdminDetail update(ContentType type, long id, UpdateRequest request) {
        ContentItem item = find(type, id);

        if (item.getVersion() != request.version()) {
            throw new ConflictException(
                    "This item was changed by someone else. Reload it and try again."
            );
        }

        validate(
                type,
                request.categoryId(),
                request.latitude(),
                request.longitude(),
                request.price(),
                request.currency()
        );

        if (items.existsByTypeAndSlugAndIdNot(type, request.slug(), id)) {
            throw new ConflictException(
                    "Slug '" + request.slug() + "' is already used by another item"
            );
        }

        item.edit(
                request.slug(),
                request.categoryId(),
                request.featured(),
                request.sortOrder(),
                request.latitude(),
                request.longitude(),
                request.price(),
                request.currency(),
                request.pricingUnit(),
                request.address(),
                request.contactPhone(),
                request.contactEmail(),
                request.externalUrl(),
                request.tags(),
                request.bestFor(),
                request.authorName(),
                request.authorUrl(),
                request.metadata()
        );

        items.saveAndFlush(item);

        return adminDetail(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSummary> adminList(
            ContentType type,
            String status,
            String q,
            int page,
            int size
    ) {
        ContentStatus wanted = parseStatus(status);

        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));

        Page<ContentItem> result = items.findAll(
                ContentSpecs.adminListing(type, wanted, q),
                PageRequest.of(
                        safePage - 1,
                        safeSize,
                        Sort.by(Sort.Order.desc("updatedAt"))
                )
        );

        Map<Long, List<ContentTranslation>> translationsByContent;

        if (result.getContent().isEmpty()) {
            translationsByContent = Map.of();
        } else {
            translationsByContent = translations.findByContentIdIn(
                            result.getContent()
                                    .stream()
                                    .map(ContentItem::getId)
                                    .toList()
                    )
                    .stream()
                    .collect(Collectors.groupingBy(
                            ContentTranslation::getContentId
                    ));
        }

        String defaultLang = languages.defaultLang();

        List<AdminSummary> summaries = result.getContent()
                .stream()
                .map(item -> {
                    List<ContentTranslation> rows =
                            translationsByContent.getOrDefault(item.getId(), List.of());

                    ContentTranslation base = rows.stream()
                            .filter(translation ->
                                    translation.getLang().equals(defaultLang)
                            )
                            .findFirst()
                            .orElse(null);

                    List<String> availableLanguages = rows.stream()
                            .map(ContentTranslation::getLang)
                            .sorted()
                            .toList();

                    return new AdminSummary(
                            item.getId(),
                            item.getSlug(),
                            item.getStatus(),
                            item.isFeatured(),
                            item.getCategoryId(),
                            base == null ? null : base.getTitle(),
                            availableLanguages,
                            item.getUpdatedAt()
                    );
                })
                .toList();

        return new PageResponse<>(
                summaries,
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminDetail adminGet(ContentType type, long id) {
        return adminDetail(find(type, id));
    }

    public AdminDetail translation(
            ContentType type,
            long id,
            String lang,
            TranslationBody body
    ) {
        ContentItem item = find(type, id);

        saveTranslation(id, languages.requireSupported(lang), body);

        return adminDetail(item);
    }

    public void deleteTranslation(ContentType type, long id, String lang) {
        find(type, id);

        String language = languages.requireSupported(lang);

        if (language.equals(languages.defaultLang())) {
            throw new ConflictException(
                    "The default-language translation cannot be removed."
            );
        }

        translations.deleteByContentIdAndLang(id, language);
    }

    public AdminDetail lifecycle(ContentType type, long id, ContentStatus status) {
        ContentItem item = find(type, id);

        if (type == ContentType.EXPERIENCE
                && status != ContentStatus.PUBLISHED
                && itineraryActivities.existsByExperienceId(id)) {
            throw new ConflictException(
                    "This experience is referenced by an itinerary. "
                            + "Update those itinerary stops before unpublishing "
                            + "or archiving it."
            );
        }

        item.setStatus(status);
        items.saveAndFlush(item);

        return adminDetail(item);
    }

    public AdminDetail replaceImages(
            ContentType type,
            long id,
            ImagesRequest request
    ) {
        ContentItem item = find(type, id);

        images.replace(
                type.ownerType(),
                id,
                request.cover(),
                request.gallery()
        );

        return adminDetail(item);
    }

    public void delete(ContentType type, long id) {
        ContentItem item = find(type, id);

        if (item.getStatus() == ContentStatus.PUBLISHED) {
            throw new ConflictException(
                    "Unpublish content before deleting it; archive is preferred."
            );
        }

        images.deleteAllFor(type.ownerType(), id);
        items.delete(item);
    }

    private void validate(
            ContentType type,
            Long categoryId,
            Double latitude,
            Double longitude,
            java.math.BigDecimal price,
            String currency
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException(
                    "latitude and longitude must be supplied together"
            );
        }

        if (price != null && (currency == null || currency.isBlank())) {
            throw new BadRequestException(
                    "currency is required when price is set"
            );
        }

        if (categoryId != null) {
            categories.requireKind(categoryId, type.categoryKind());
        }
    }

    private void saveTranslation(
            Long id,
            String lang,
            TranslationBody body
    ) {
        if (!body.body().isArray()
                || !body.highlights().isArray()
                || !body.tips().isArray()) {
            throw new BadRequestException(
                    "body, highlights, and tips must be JSON arrays."
            );
        }

        ContentTranslation translation = translations
                .findByContentIdAndLang(id, lang)
                .orElseGet(() -> new ContentTranslation(id, lang));

        translation.edit(
                body.title(),
                body.excerpt(),
                body.body(),
                body.highlights(),
                body.tips(),
                body.seoTitle(),
                body.seoDescription(),
                body.confirmedInformation()
        );

        translations.save(translation);
    }

    private ContentItem find(ContentType type, long id) {
        ContentItem item = items.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Content " + id + " was not found")
                );

        if (item.getType() != type) {
            throw new NotFoundException("Content " + id + " was not found");
        }

        return item;
    }

    private ContentStatus parseStatus(String status) {
        if (status == null
                || status.isBlank()
                || status.equalsIgnoreCase("all")) {
            return null;
        }

        try {
            return ContentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "status must be all, draft, published, or archived"
            );
        }
    }


    private record Lookups(
            Map<Long, ContentTranslation> text,
            Map<Long, CategoryDto> categories,
            Map<Long, OwnerImages> images
    ) {
        ContentCard card(ContentItem item) {
            ContentTranslation translation = text.get(item.getId());

            OwnerImages media = images.getOrDefault(
                    item.getId(),
                    new OwnerImages(null, List.of())
            );

            // Category is optional, so never call Map.get(null).
            CategoryDto category = item.getCategoryId() == null
                    ? null
                    : categories.get(item.getCategoryId());

            return new ContentCard(
                    item.getSlug(),
                    translation.getTitle(),
                    translation.getExcerpt(),
                    category,
                    media.cover(),
                    item.getTags(),
                    item.getBestFor(),
                    item.isFeatured(),
                    item.getPublishedAt(),
                    translation.getLang()
            );
        }
    }


    private Lookups lookups(Collection<ContentItem> content, String lang) {
        if (content.isEmpty()) {
            return new Lookups(Map.of(), Map.of(), Map.of());
        }

        List<Long> ids = content.stream()
                .map(ContentItem::getId)
                .toList();

        String defaultLang = languages.defaultLang();

        // Avoid duplicate language values when lang equals defaultLang.
        Set<String> requestedLanguages = new LinkedHashSet<>();
        requestedLanguages.add(lang);
        requestedLanguages.add(defaultLang);

        Map<Long, ContentTranslation> picked = new HashMap<>();

        for (ContentTranslation translation :
                translations.findByContentIdInAndLangIn(ids, requestedLanguages)) {

            Long contentId = translation.getContentId();

            // Requested language always takes priority over the fallback.
            if (translation.getLang().equals(lang)
                    || !picked.containsKey(contentId)) {
                picked.put(contentId, translation);
            }
        }

        Set<Long> categoryIds = content.stream()
                .map(ContentItem::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        ContentType type = content.iterator().next().getType();

        return new Lookups(
                picked,
                categories.byIds(categoryIds, lang),
                images.publicImages(type.ownerType(), ids, lang)
        );
    }

    private AdminDetail adminDetail(ContentItem item) {
        Map<String, TranslationBody> allTranslations = new TreeMap<>();

        translations.findByContentId(item.getId())
                .forEach(translation ->
                        allTranslations.put(
                                translation.getLang(),
                                new TranslationBody(
                                        translation.getTitle(),
                                        translation.getExcerpt(),
                                        translation.getBody(),
                                        translation.getHighlights(),
                                        translation.getTips(),
                                        translation.getSeoTitle(),
                                        translation.getSeoDescription(),
                                        translation.isConfirmedInformation()
                                )
                        )
                );

        return new AdminDetail(
                item.getId(),
                item.getVersion(),
                item.getSlug(),
                item.getStatus(),
                item.isFeatured(),
                item.getSortOrder(),
                item.getCategoryId(),
                item.getLatitude(),
                item.getLongitude(),
                item.getPrice(),
                item.getCurrency(),
                item.getPricingUnit(),
                item.getAddress(),
                item.getContactPhone(),
                item.getContactEmail(),
                item.getExternalUrl(),
                item.getTags(),
                item.getBestFor(),
                item.getAuthorName(),
                item.getAuthorUrl(),
                item.getMetadata(),
                item.getPublishedAt(),
                item.getUpdatedAt(),
                images.adminImages(item.getType().ownerType(), item.getId()),
                allTranslations
        );
    }
}
