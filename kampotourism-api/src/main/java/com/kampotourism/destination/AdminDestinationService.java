package com.kampotourism.destination;

import com.kampotourism.category.CategoryKind;
import com.kampotourism.category.CategoryService;
import com.kampotourism.common.PageResponse;
import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.common.error.ConflictException;
import com.kampotourism.common.error.NotFoundException;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.destination.AdminDestinationDtos.AdminDestinationDetail;
import com.kampotourism.destination.AdminDestinationDtos.AdminDestinationSummary;
import com.kampotourism.destination.AdminDestinationDtos.CreateDestinationRequest;
import com.kampotourism.destination.AdminDestinationDtos.ImagesRequest;
import com.kampotourism.destination.AdminDestinationDtos.TranslationBody;
import com.kampotourism.destination.AdminDestinationDtos.UpdateDestinationRequest;
import com.kampotourism.media.MediaAttachmentService;
import com.kampotourism.media.OwnerType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Editor-facing destination management: drafts, translations, images, publishing. */
@Service
@Transactional
public class AdminDestinationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DestinationRepository destinations;
    private final DestinationTranslationRepository translations;
    private final CategoryService categories;
    private final MediaAttachmentService images;
    private final LanguageResolver languages;

    public AdminDestinationService(DestinationRepository destinations,
                                   DestinationTranslationRepository translations, CategoryService categories,
                                   MediaAttachmentService images, LanguageResolver languages) {
        this.destinations = destinations;
        this.translations = translations;
        this.categories = categories;
        this.images = images;
        this.languages = languages;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDestinationSummary> list(String status, String q, Long categoryId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Boolean published = switch (status == null ? "all" : status.toLowerCase()) {
            case "all" -> null;
            case "published" -> true;
            case "draft" -> false;
            default -> throw new BadRequestException("status must be one of: all, published, draft");
        };

        Page<Destination> result = destinations.findAll(
                DestinationSpecs.admin(published, categoryId, q),
                PageRequest.of(safePage - 1, safeSize,
                        Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"))));

        Collection<Long> ids = result.getContent().stream().map(Destination::getId).toList();
        Map<Long, List<DestinationTranslation>> byDestination = ids.isEmpty() ? Map.of()
                : translations.findByDestinationIdIn(ids).stream()
                        .collect(Collectors.groupingBy(DestinationTranslation::getDestinationId));

        List<AdminDestinationSummary> items = result.getContent().stream().map(d -> {
            List<DestinationTranslation> texts = byDestination.getOrDefault(d.getId(), List.of());
            String name = texts.stream().filter(t -> t.getLang().equals(languages.defaultLang()))
                    .findFirst().or(() -> texts.stream().findFirst())
                    .map(DestinationTranslation::getName).orElse(d.getSlug());
            List<String> langs = texts.stream().map(DestinationTranslation::getLang).sorted().toList();
            return new AdminDestinationSummary(d.getId(), d.getSlug(), name, d.getCategoryId(), d.isPublished(),
                    d.isFeatured(), d.getSortOrder(), d.getUpdatedAt(), langs);
        }).toList();
        return new PageResponse<>(items, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminDestinationDetail get(long id) {
        return detail(find(id));
    }

    /** New destinations start as drafts and need the default-language text up front. */
    public AdminDestinationDetail create(CreateDestinationRequest r) {
        if (destinations.existsBySlug(r.slug())) {
            throw new ConflictException("A destination with slug '%s' already exists".formatted(r.slug()));
        }
        categories.requireKind(r.categoryId(), CategoryKind.DESTINATION);
        checkCoordinates(r.latitude(), r.longitude());
        checkFee(r.entranceFee(), r.entranceFeeCurrency());

        Destination destination = new Destination();
        destination.edit(r.slug(), r.categoryId(), r.latitude(), r.longitude(), r.entranceFee(),
                r.entranceFeeCurrency(), r.featured(), r.sortOrder());
        destination = destinations.saveAndFlush(destination);

        DestinationTranslation text = new DestinationTranslation(destination.getId(), languages.defaultLang());
        AdminDestinationMapper.apply(text, r.translation());
        translations.save(text);
        return detail(destination);
    }

    public AdminDestinationDetail update(long id, UpdateDestinationRequest r) {
        Destination destination = find(id);
        if (destination.getVersion() != r.version()) {
            throw new ConflictException("This destination was changed by someone else. Reload it and try again.");
        }
        if (destinations.existsBySlugAndIdNot(r.slug(), id)) {
            throw new ConflictException("Slug '%s' is already used by another destination".formatted(r.slug()));
        }
        categories.requireKind(r.categoryId(), CategoryKind.DESTINATION);
        checkCoordinates(r.latitude(), r.longitude());
        checkFee(r.entranceFee(), r.entranceFeeCurrency());

        destination.edit(r.slug(), r.categoryId(), r.latitude(), r.longitude(), r.entranceFee(),
                r.entranceFeeCurrency(), r.featured(), r.sortOrder());
        destinations.saveAndFlush(destination);
        return detail(destination);
    }

    public AdminDestinationDetail upsertTranslation(long id, String lang, TranslationBody body) {
        Destination destination = find(id);
        String normalized = languages.requireSupported(lang);
        DestinationTranslation text = translations.findByDestinationIdAndLang(id, normalized)
                .orElseGet(() -> new DestinationTranslation(id, normalized));
        AdminDestinationMapper.apply(text, body);
        translations.save(text);
        return detail(destination);
    }

    public void deleteTranslation(long id, String lang) {
        find(id);
        String normalized = languages.requireSupported(lang);
        if (normalized.equals(languages.defaultLang())) {
            throw new ConflictException("The default-language (%s) text cannot be removed.".formatted(normalized));
        }
        translations.deleteByDestinationIdAndLang(id, normalized);
    }

    public AdminDestinationDetail setPublished(long id, boolean published) {
        Destination destination = find(id);
        if (published && translations.findByDestinationIdAndLang(id, languages.defaultLang()).isEmpty()) {
            throw new ConflictException("Add the %s text before publishing.".formatted(languages.defaultLang()));
        }
        destination.setPublished(published);
        destinations.saveAndFlush(destination);
        return detail(destination);
    }

    /** Replaces cover + gallery in one call (also handles reordering and removal). */
    public AdminDestinationDetail replaceImages(long id, ImagesRequest request) {
        Destination destination = find(id);
        images.replace(OwnerType.DESTINATION, id, request.cover(), request.gallery());
        return detail(destination);
    }

    /** A live destination must be unpublished first, so a published page can never vanish by accident. */
    public void delete(long id) {
        Destination destination = find(id);
        if (destination.isPublished()) {
            throw new ConflictException("Unpublish this destination before deleting it.");
        }
        images.deleteAllFor(OwnerType.DESTINATION, id);
        destinations.delete(destination);
        destinations.flush();
    }

    private AdminDestinationDetail detail(Destination d) {
        Map<String, TranslationBody> texts = new TreeMap<>();
        translations.findByDestinationId(d.getId())
                .forEach(t -> texts.put(t.getLang(), AdminDestinationMapper.toBody(t)));
        return new AdminDestinationDetail(d.getId(), d.getVersion(), d.getSlug(), d.getCategoryId(),
                d.getLatitude(), d.getLongitude(), d.getEntranceFee(), d.getEntranceFeeCurrency(),
                d.isFeatured(), d.isPublished(), d.getSortOrder(), d.getUpdatedAt(),
                images.adminImages(OwnerType.DESTINATION, d.getId()), texts);
    }

    private Destination find(long id) {
        return destinations.findById(id)
                .orElseThrow(() -> new NotFoundException("Destination %d was not found".formatted(id)));
    }

    private static void checkCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("Provide both latitude and longitude, or neither.");
        }
    }

    private static void checkFee(BigDecimal fee, String currency) {
        if (fee != null && currency == null) {
            throw new BadRequestException("entranceFeeCurrency is required when entranceFee is set.");
        }
    }
}
