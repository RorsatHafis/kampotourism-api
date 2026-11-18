package com.kampotourism.media;

import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.media.MediaDtos.AdminImageRef;
import com.kampotourism.media.MediaDtos.AdminImages;
import com.kampotourism.media.MediaDtos.ImageDto;
import com.kampotourism.media.MediaDtos.OwnerImages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place that knows how images are attached to content. Destinations, hotels,
 * blogs and the rest all call this instead of re-implementing galleries.
 */
@Service
public class MediaAttachmentService {

    private final MediaAttachmentRepository attachments;
    private final MediaAssetRepository assets;
    private final MediaAssetTranslationRepository texts;
    private final MediaUrls urls;
    private final LanguageResolver languages;

    public MediaAttachmentService(MediaAttachmentRepository attachments, MediaAssetRepository assets,
                                  MediaAssetTranslationRepository texts, MediaUrls urls,
                                  LanguageResolver languages) {
        this.attachments = attachments;
        this.assets = assets;
        this.texts = texts;
        this.urls = urls;
        this.languages = languages;
    }

    /**
     * Replaces the owner's whole image set: one optional cover plus an ordered gallery.
     * Sending the full list keeps reordering and removal trivial for the admin UI.
     */
    @Transactional
    public void replace(OwnerType type, Long ownerId, Long coverId, List<Long> galleryIds) {
        List<Long> gallery = galleryIds == null ? List.of() : galleryIds;
        Set<Long> galleryOrdered = new LinkedHashSet<>(gallery);
        if (galleryOrdered.size() != gallery.size()) {
            throw new BadRequestException("The gallery contains the same image more than once.");
        }
        if (coverId != null && galleryOrdered.contains(coverId)) {
            throw new BadRequestException("The cover image must not also be in the gallery.");
        }

        Set<Long> all = new LinkedHashSet<>();
        if (coverId != null) {
            all.add(coverId);
        }
        all.addAll(galleryOrdered);
        validateAssets(all);

        attachments.deleteByOwnerTypeAndOwnerId(type, ownerId);
        attachments.flush();

        int order = 0;
        if (coverId != null) {
            attachments.save(new MediaAttachment(type, ownerId, coverId, AttachmentRole.COVER, order++));
        }
        for (Long id : galleryOrdered) {
            attachments.save(new MediaAttachment(type, ownerId, id, AttachmentRole.GALLERY, order++));
        }
    }

    @Transactional
    public void deleteAllFor(OwnerType type, Long ownerId) {
        attachments.deleteByOwnerTypeAndOwnerId(type, ownerId);
    }

    /** Public view: images with alt text and captions in the requested language (falling back to the default). */
    @Transactional(readOnly = true)
    public Map<Long, OwnerImages> publicImages(OwnerType type, Collection<Long> ownerIds, String lang) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        List<MediaAttachment> links =
                attachments.findByOwnerTypeAndOwnerIdInOrderBySortOrderAscIdAsc(type, ownerIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        Set<Long> assetIds = links.stream().map(MediaAttachment::getAssetId).collect(Collectors.toSet());
        Map<Long, MediaAsset> assetById = new HashMap<>();
        assets.findAllById(assetIds).forEach(a -> assetById.put(a.getId(), a));
        Map<Long, MediaAssetTranslation> textById = pickTexts(assetIds, lang);

        Map<Long, List<MediaAttachment>> byOwner =
                links.stream().collect(Collectors.groupingBy(MediaAttachment::getOwnerId));
        Map<Long, OwnerImages> result = new HashMap<>();
        byOwner.forEach((ownerId, ownerLinks) -> {
            ImageDto cover = null;
            List<ImageDto> gallery = new ArrayList<>();
            for (MediaAttachment link : ownerLinks) {
                MediaAsset asset = assetById.get(link.getAssetId());
                if (asset == null) {
                    continue;
                }
                MediaAssetTranslation text = textById.get(asset.getId());
                ImageDto image = new ImageDto(urls.url(asset.getStorageKey()),
                        text == null ? null : text.getAltText(),
                        text == null ? null : text.getCaption(),
                        asset.getWidth(), asset.getHeight());
                if (link.getRole() == AttachmentRole.COVER) {
                    cover = image;
                } else {
                    gallery.add(image);
                }
            }
            result.put(ownerId, new OwnerImages(cover, gallery));
        });
        return result;
    }

    /** Admin view: same images but with media ids (so the UI can reorder/detach) and default-language texts. */
    @Transactional(readOnly = true)
    public AdminImages adminImages(OwnerType type, Long ownerId) {
        List<MediaAttachment> links =
                attachments.findByOwnerTypeAndOwnerIdInOrderBySortOrderAscIdAsc(type, List.of(ownerId));
        if (links.isEmpty()) {
            return new AdminImages(null, List.of());
        }
        Set<Long> assetIds = links.stream().map(MediaAttachment::getAssetId).collect(Collectors.toSet());
        Map<Long, MediaAsset> assetById = new HashMap<>();
        assets.findAllById(assetIds).forEach(a -> assetById.put(a.getId(), a));
        Map<Long, MediaAssetTranslation> textById = pickTexts(assetIds, languages.defaultLang());

        AdminImageRef cover = null;
        List<AdminImageRef> gallery = new ArrayList<>();
        for (MediaAttachment link : links) {
            MediaAsset asset = assetById.get(link.getAssetId());
            if (asset == null) {
                continue;
            }
            MediaAssetTranslation text = textById.get(asset.getId());
            AdminImageRef ref = new AdminImageRef(asset.getId(), urls.url(asset.getStorageKey()),
                    text == null ? null : text.getAltText(), text == null ? null : text.getCaption());
            if (link.getRole() == AttachmentRole.COVER) {
                cover = ref;
            } else {
                gallery.add(ref);
            }
        }
        return new AdminImages(cover, gallery);
    }

    private void validateAssets(Set<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<Long> found = assets.findAllById(ids).stream().map(MediaAsset::getId).collect(Collectors.toSet());
        List<Long> unknown = ids.stream().filter(id -> !found.contains(id)).toList();
        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown media id(s): " + unknown);
        }
        String defaultLang = languages.defaultLang();
        Set<Long> withAlt = texts.findByAssetIdInAndLangIn(ids, Set.of(defaultLang)).stream()
                .filter(t -> t.getAltText() != null && !t.getAltText().isBlank())
                .map(MediaAssetTranslation::getAssetId)
                .collect(Collectors.toSet());
        List<Long> missing = ids.stream().filter(id -> !withAlt.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("Add alt text (%s) to image(s) %s before using them."
                    .formatted(defaultLang, missing));
        }
    }

    private Map<Long, MediaAssetTranslation> pickTexts(Collection<Long> assetIds, String lang) {
        Set<String> wanted = Set.copyOf(List.of(lang, languages.defaultLang()));
        Map<Long, MediaAssetTranslation> picked = new HashMap<>();
        for (MediaAssetTranslation t : texts.findByAssetIdInAndLangIn(assetIds, wanted)) {
            if (t.getLang().equals(lang) || !picked.containsKey(t.getAssetId())) {
                picked.put(t.getAssetId(), t);
            }
        }
        return picked;
    }
}
